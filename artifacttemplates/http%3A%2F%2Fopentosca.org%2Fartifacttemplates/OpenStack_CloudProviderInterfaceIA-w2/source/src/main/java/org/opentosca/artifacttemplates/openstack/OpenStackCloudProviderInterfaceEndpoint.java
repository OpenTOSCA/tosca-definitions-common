package org.opentosca.artifacttemplates.openstack;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.*;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.image.v2.ContainerFormat;
import org.openstack4j.model.image.v2.DiskFormat;
import org.openstack4j.model.network.Network;
import org.openstack4j.openstack.OSFactory;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;
import org.opentosca.artifacttemplates.openstack.model.CreateVMRequest;
import org.opentosca.artifacttemplates.openstack.model.InvokeResponse;
import org.opentosca.artifacttemplates.openstack.model.OpenStackRequest;
import org.opentosca.artifacttemplates.openstack.model.TerminateVMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import static org.openstack4j.core.transport.Config.newConfig;

@Endpoint
public class OpenStackCloudProviderInterfaceEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(OpenStackCloudProviderInterfaceEndpoint.class);

    private final static List<SupportedArtifactType> SUPPORTED_ARTIFACT_TYPE_TYPES = Arrays.asList(
            new SupportedArtifactType(
                    QName.valueOf("{http://opentosca.org/artifacttypes}ISO"),
                    "iso",
                    DiskFormat.RAW
            ),
            new SupportedArtifactType(
                    QName.valueOf("{http://opentosca.org/artifacttypes}CloudImage"),
                    "img",
                    DiskFormat.QCOW2
            )
    );

    @PayloadRoot(namespace = OpenStackConstants.NAMESPACE_URI, localPart = "createVMRequest")
    public void createVM(@RequestPayload CreateVMRequest request, MessageContext messageContext) {

            logger.info("Received create VM request!");

            OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

            InvokeResponse response = new InvokeResponse();
            response.setMessageID(openToscaHeaders.messageId());

            OSFactory.enableHttpLoggingFilter(true);

            String isoLocation = null;
            SupportedArtifactType supportedArtifactType = null;

        try {
            if (openToscaHeaders.deploymentArtifacts().isEmpty()) {
                logger.info("Did not receive any attached DeploymentArtifacts!");
            } else {
                Optional<SupportedArtifactType> optional = SUPPORTED_ARTIFACT_TYPE_TYPES.stream()
                        .filter(artifactType ->
                                openToscaHeaders.deploymentArtifacts().containsKey(artifactType.artifactType)
                        ).findFirst();

                if (optional.isPresent()) {
                    supportedArtifactType = optional.get();
                    logger.info("Found supported Artifact Type {}", supportedArtifactType);
                    Map<String, String> deploymentArtifactLocations = openToscaHeaders.deploymentArtifacts()
                            .get(supportedArtifactType.artifactType);

                    for (Map.Entry<String, String> deploymentArtifactLocation : deploymentArtifactLocations.entrySet()) {
                        if (deploymentArtifactLocation.getKey().endsWith(supportedArtifactType.fileType)) {
                            isoLocation = deploymentArtifactLocation.getValue();
                            logger.info("Found a {} file available at {}", supportedArtifactType.fileType.toUpperCase(), isoLocation);
                            break;
                        } else {
                            logger.warn("Found non matching file {} for artifact type {}",
                                    deploymentArtifactLocation.getKey(), supportedArtifactType);
                        }
                    }
                }
            }

            // Create OpenStack client
            OSClient<?> osClient = authenticate(request);

            if (osClient == null) {
                response.setError("Could not connect to OpenStack Instance at " + request.getHypervisorEndpoint());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                logger.error("Could not create connection...");
                return;
            } else {
                logger.info("Successfully authenticated at {}", request.getHypervisorEndpoint());
            }

            org.openstack4j.model.image.v2.Image uploadedImage = null;
            try {
                if (supportedArtifactType != null && isoLocation != null && !isoLocation.isEmpty()) {
                    logger.info("{} file is attached! Trying to create image with format '{}'",
                            supportedArtifactType.fileType.toUpperCase(), supportedArtifactType.diskFormat.value());
                    Payload<URL> payload = Payloads.create(new URL(isoLocation));

                    String generatedImageId = "opentosca-" + request.getVMImageID()
                            .replaceAll("\\s", "") + "-" + System.currentTimeMillis();
                    logger.info("Creating image with name '{}'", generatedImageId);

                    uploadedImage = osClient.imagesV2().create(Builders.imageV2()
                            .name(generatedImageId)
                            .containerFormat(ContainerFormat.BARE)
                            .diskFormat(supportedArtifactType.diskFormat)
                            .minDisk(3L)
                            .visibility(org.openstack4j.model.image.v2.Image.ImageVisibility.PRIVATE)
                            .build());

                    long startUpload = System.currentTimeMillis();
                    logger.info("Starting to upload file...");
                    ActionResponse upload = osClient.imagesV2().upload(
                            uploadedImage.getId(),
                            payload,
                            uploadedImage);
                    long duration = (System.currentTimeMillis() - startUpload) / 1000;
                    logger.info("Uploading success: {}", upload.isSuccess());
                    logger.info("Uploading lasted {}min {}s", (int) duration / 60, duration % 60);
                }
            } catch (Exception e) {
                logger.error("An error happened while creating an image from the attached file!", e);
                response.setError("Could not upload image");

                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                return;
            }

            // add defined security group or create new security group with defined open ports
            String securityGroup;
            if (request.getVMSecurityGroup() != null && !request.getVMSecurityGroup().isEmpty()) {
                logger.info("Adding configured security group: {}", request.getVMSecurityGroup());
                securityGroup = request.getVMSecurityGroup();
            } else{
                logger.info("Creating new security group to open ports: {}", request.getVMOpenPorts());

                // create security group
                SecGroupExtension group = osClient.compute().securityGroups().create("OpenTOSCA-" + System.currentTimeMillis(), "OpenTOSCA security group");
                securityGroup = group.getName();
                logger.info("Created new security group with name: {}", securityGroup);

                // open ports within security group
                List<String> ports = new ArrayList<>(List.of(request.getVMOpenPorts().split(",")));
                if (!ports.contains("22")){
                    // add SSH port if not defined
                    ports.add("22");
                }
                logger.info("Opening {} ports...", ports.size());
                for (String port : ports){
                    logger.info("Opening port: {}", port);
                    osClient.compute().securityGroups()
                            .createRule(Builders.secGroupRule()
                                    .parentGroupId(group.getId())
                                    .protocol(IPProtocol.TCP)
                                    .cidr("0.0.0.0/0")
                                    .range(Integer.parseInt(port), Integer.parseInt(port)).build());
                }
            }
            logger.info("Resulting security group: {}", securityGroup);

            // Get Networks based on Type String
            List<? extends Network> availableNetworks = osClient.networking().network().list();
            logger.info("Found "+ availableNetworks.size() + " Networks");
            logger.info("Searching for Network: " + request.getVMNetworks());
            List<String> availableNetworksIds = availableNetworks.stream().map(IdEntity::getId).filter(id -> Arrays.asList(request.getVMNetworks().split(",")).contains(id)).toList();

            if (availableNetworksIds.isEmpty()) {
                response.setError("Cannot find matching network for input " + request.getVMNetworks());
                logger.error("Cannot find matching network for input " + request.getVMNetworks());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());

                throw new RuntimeException("Cannot find matching network for input " + request.getVMNetworks());
            }
            logger.info("Using {} networks: {}", availableNetworksIds.size(), availableNetworksIds);

            // Get Flavor based on Type String
            List<? extends Flavor> flavours = osClient.compute().flavors().list();
            Flavor flavor = null;
            for (Flavor f : flavours) {
                if (f.getId().equals(request.getVMType()) || f.getName().equals(request.getVMType())) {
                    flavor = f;
                    break;
                }
            }
            if (flavor == null) {
                response.setError("Cannot find flavor for input " + request.getVMType());
                logger.error("Cannot find flavor for input {}", request.getVMType());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());

                throw new RuntimeException("Cannot find flavor for input " + request.getVMType());
            }
            logger.info("Found flavour {}", flavor.getName());

            String[] imageIdParts = request.getVMImageID().split("-");
            // Get Flavor based on Type String
            List<? extends Image> images = osClient.compute().images().list();
            Image image;

            Optional<? extends Image> optionalImage;

            if (uploadedImage == null) {
                optionalImage = images.stream()
                        .filter(img -> {
                            final String imageName = img.getName().toLowerCase();
                            return imageName.equals(request.getVMImageID().toLowerCase()) || imageName.startsWith(request.getVMImageID())
                                    || Arrays.stream(imageIdParts).allMatch(part -> imageName.contains(part.toLowerCase()));
                        })
                        .findFirst();
            } else {
                org.openstack4j.model.image.v2.Image finalImage = uploadedImage;
                optionalImage = images.stream()
                        .filter(img -> img.getId().equals(finalImage.getId()) || img.getName().equals(finalImage.getName()))
                        .findFirst();
            }

            if (optionalImage.isPresent()) {
                image = optionalImage.get();
                logger.info("Found image to use \"{}\"", image.getName());
            } else {
                response.setError("Cannot find image for input " + request.getVMImageID());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());

                throw new RuntimeException("Cannot find image for input " + request.getVMImageID());
            }

            // Build the server
            ServerCreateBuilder serverCreateBuilder = Builders.server()
                    .name("OpenTOSCA-" + System.currentTimeMillis())
                    .flavor(flavor)
                    .image(image)
                    .networks(availableNetworksIds)
                    .addSecurityGroup(securityGroup)
                    .keypairName(request.getVMKeyPairName());

            ServerCreate sc = serverCreateBuilder.build();

            // Start Server
            Server server = osClient.compute()
                    .servers()
                    .boot(sc);
            logger.info("Started server with ID {}", server.getId());

            // Retrying for some minutes for OpenStack to set up the server
            int i = 0, max = 60;
            do {
                logger.info("Waiting 5s for server to come up... {}/{}", ++i, max);
                try {
                    Thread.sleep(5000); // wait for 5 sec
                } catch (InterruptedException e) {
                    // we just go on in this case.
                }

                // Get server's information
                server = osClient.compute()
                        .servers()
                        .get(server.getId());

                if (server.getStatus().equals(Status.ERROR)) {
                    // An error occurred
                    logger.error("Failed to start server...");
                    throw new RuntimeException("Failed to start server.");
                } else if (server.getStatus().equals(Status.ACTIVE)) {
                    // Ok, it's done, we can go on
                    logger.info("Server is active!");
                    break;
                }
            } while (i <= max);

            // Get server's fixed IP from the list of addresses
            String serversInternalIP = null;
            for (List<? extends Address> addressesOfNetwork : server.getAddresses().getAddresses().values()) {
                for (Address address : addressesOfNetwork) {
                    if (address.getType().equals("fixed")) {
                        serversInternalIP = address.getAddr();
                        logger.info("Found fixed IP-Address: {}", serversInternalIP);
                        break;
                    }
                }
            }

            // Try to find a Floating IP which is not assigned to any instance yet.
            String floatingIp = null;
            for (FloatingIP fip : osClient.compute().floatingIps().list()) {
                if (fip.getInstanceId() == null) {
                    floatingIp = fip.getFloatingIpAddress();
                    logger.info("Found a FloatingIP with is not assigned: {}", floatingIp);
                    break;
                }
            }

            // If there is no free FloatingIP, we try to assign one from the first
            // pools which allows us to allocate one.
            if (floatingIp == null) {
                List<String> poolNames = osClient.compute().floatingIps().getPoolNames();
                for (String poolName : poolNames) {
                    try {
                        // Try to allocate IP from this pool
                        floatingIp = osClient.compute().floatingIps().allocateIP(poolName).getFloatingIpAddress();
                        // If it worked, we stop here
                        if (floatingIp != null) {
                            logger.info(
                                    "Allocated new FloatingIP {} from pool {} because there was no FloatingIP available which was not assigned yet.",
                                    floatingIp, poolName
                            );
                            break;
                        }
                    } catch (Exception e) {
                        logger.warn("Unable to allocate FloatingIP from pool {}. Error was '{}'. Trying next pool.", poolName, e.getMessage());
                        logger.debug("Unable to allocate FloatingIP", e);
                    }
                }
            }

            // Have we been able to find a FloatingIP?
            if (floatingIp == null) {
                logger.error("Unable to find and allocate a FloatingIP. Machine will be started without floating IP and, therefore, might not be accessible from the outside.");
                // If not, we are setting the server's internal IP as floating IP,
                // so it is returned to the user, even if it might not be accessible.
                floatingIp = serversInternalIP;
            } else {
                // Assign Floating IP
                osClient.compute().floatingIps().addFloatingIP(server, floatingIp);
            }

            // Output Parameters
            response.setVMInstanceID(server.getId());
            response.setVMIP(floatingIp);

            logger.info("Successfully started VM with internal IP {} and public IP {}", serversInternalIP, floatingIp);

            SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
        } catch (Exception e){
            response.setError("Exception occurred: " + e.getMessage());
            e.printStackTrace();
            SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());

            throw new RuntimeException("Exception occurred: " + e.getMessage());
        }
    }

    @PayloadRoot(namespace = OpenStackConstants.NAMESPACE_URI, localPart = "terminateVMRequest")
    public void terminateVM(@RequestPayload TerminateVMRequest request, MessageContext messageContext) {
        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());

        // Create OpenStack client
        OSClient<?> osClient = authenticate(request);
        logger.info("Successfully authenticated at {}", request.getHypervisorEndpoint());

        if (osClient == null) {
            response.setError("Could not connect to OpenStack Instance at " + request.getHypervisorEndpoint());
            SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
            logger.error("Could not create connection...");
            return;
        }

        // Get the server object
        Server server = null;
        for (Server s : osClient.compute().servers().list()) {
            // Search by ID and Name (this is more than advertised in the
            // interface, i.e. publicDNSorInstanceId)
            if (s.getId().equals(request.getVMInstanceID()) || s.getName().equals(request.getVMInstanceID())) {
                server = s;
                break;
            }

            // Search by IP
            for (List<? extends Address> addressesOfNetwork : s.getAddresses().getAddresses().values()) {
                for (Address address : addressesOfNetwork) {
                    if (address.getAddr().equals(request.getVMInstanceID())) {
                        server = s;
                        break;
                    }
                }
            }
        }

        if (server == null) {
            response.setError("Could not find server with ID \"" + request.getVMInstanceID() + "\"");
            logger.info("Could not find server with ID \"{}\"", request.getVMInstanceID());
        } else {
            // Shutdown and delete this server
            osClient.compute().servers().delete(server.getId());
            response.setResult("true");
            logger.info("Successfully terminated server with ID \"{}}\"", request.getVMInstanceID());
        }

        SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
    }

    /**
     * Creates an OpenStack client from the input data
     *
     * @param openStackRequest - the request object
     * @return Authenticated OpenStack Client
     */
    OSClient<?> authenticate(OpenStackRequest openStackRequest) {
        Config config = newConfig()
                .withSSLVerificationDisabled();

        // Authenticate with OpenStack and explicitly using v3
        String endpoint = openStackRequest.getHypervisorEndpoint().endsWith("/v3") ? "" : ":5000/v3";
        if (openStackRequest.getHypervisorEndpoint().startsWith("http")) {
            endpoint = openStackRequest.getHypervisorEndpoint() + endpoint;
        } else {
            endpoint = "https://" + openStackRequest.getHypervisorEndpoint() + endpoint;
        }

        // We prefer the application_credential authentication method over the username/password.
        if (openStackRequest.getHypervisorApplicationID() != null && !openStackRequest.getHypervisorApplicationID().isBlank()
                && openStackRequest.getHypervisorApplicationSecret() != null && !openStackRequest.getHypervisorApplicationSecret().isBlank()) {
            logger.info("Connecting to \"{}\" using the application credentials...", endpoint);

            try {
                return OSFactory.builderV3()
                        .withConfig(config)
                        .endpoint(endpoint)
                        .applicationCredentials(openStackRequest.getHypervisorApplicationID(), openStackRequest.getHypervisorApplicationSecret())
                        .authenticate()
                        .useRegion(openStackRequest.getHypervisorRegion());
            } catch (Exception e) {
                logger.error("Error while authenticating at {}", endpoint, e);
            }
        } else if (openStackRequest.getHypervisorUserName() != null && !openStackRequest.getHypervisorUserName().isBlank()
                && openStackRequest.getHypervisorUserPassword() != null && !openStackRequest.getHypervisorUserPassword().isBlank()) {
            logger.info("Connecting to \"{}\" using username and password...", endpoint);
            logger.warn("""
                    This method is not recommended as your password is used. Instead use the application credentials!
                    More information can be found here https://docs.openstack.org/keystone/queens/user/application_credentials.html
                    """);

            try {
                return OSFactory.builderV3()
                        .withConfig(config)
                        .perspective(Facing.PUBLIC)
                        .endpoint(endpoint)
                        .credentials(openStackRequest.getHypervisorUserName(), openStackRequest.getHypervisorUserPassword(), Identifier.byName("Default"))
                        .scopeToProject(Identifier.byId(openStackRequest.getHypervisorTenantID()))
                        .authenticate()
                        .useRegion(openStackRequest.getHypervisorRegion());
            } catch (Exception e) {
                logger.error("Error while authenticating at {}", endpoint, e);
            }
        }

        return null;
    }

    private record SupportedArtifactType(QName artifactType,
                                         String fileType,
                                         DiskFormat diskFormat) {
    }
}
