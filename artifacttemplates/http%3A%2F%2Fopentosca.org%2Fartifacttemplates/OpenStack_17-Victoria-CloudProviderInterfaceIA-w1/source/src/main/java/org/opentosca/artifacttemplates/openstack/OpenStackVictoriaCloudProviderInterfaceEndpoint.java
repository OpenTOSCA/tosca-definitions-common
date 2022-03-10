package org.opentosca.artifacttemplates.openstack;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder.V3;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.image.v2.ContainerFormat;
import org.openstack4j.model.image.v2.DiskFormat;
import org.openstack4j.openstack.OSFactory;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import static org.openstack4j.core.transport.Config.newConfig;

@Endpoint
public class OpenStackVictoriaCloudProviderInterfaceEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(OpenStackVictoriaCloudProviderInterfaceEndpoint.class);

    private final static List<SupportedArtifactType> SUPPORTED_ARTIFACT_TYPE_TYPES = Arrays.asList(
            new SupportedArtifactType("{http://opentosca.org/artifacttypes}ISO", "iso", DiskFormat.RAW),
            new SupportedArtifactType("{http://opentosca.org/artifacttypes}CloudImage", "img", DiskFormat.QCOW2)
    );

    @Resource
    private WebServiceContext context;

    @PayloadRoot(namespace = OpenStackConstants.NAMESPACE_URI, localPart = "createVMRequest")
    public void createVM(@RequestPayload CreateVMRequest request, MessageContext messageContext) {
        logger.info("Received create VM request!");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());

        OSFactory.enableHttpLoggingFilter(true);
        SupportedArtifactType artifactType = SUPPORTED_ARTIFACT_TYPE_TYPES.get(1);
//        Gson gson = new Gson();
//
        String isoLocation = null;
////        isoLocation = "file:///C:/Users/lharz/Downloads/bionic-server-cloudimg-amd64.img";
//
//        // Extract message
//        WrappedMessageContext wrappedContext = (WrappedMessageContext) context.getMessageContext();
//
//        Message message = wrappedContext.getWrappedMessage();
//
//        // Extract headers from message
//        List<Header> headers = CastUtils.cast((List<?>) message.get(Header.HEADER_LIST));
//
//        for (Header header : headers) {
//            Object headerObject = header.getObject();
//            logger.info("Found header: '{}' and content '{}'", header.getName(), headerObject);
//
//            // Unmarshall to org.w3c.dom.Node
//            if (headerObject instanceof Node) {
//                Node node = (Node) headerObject;
//                String localPart = header.getName().getLocalPart();
//                String content = node.getTextContent();
//
//                // Extract DEPLOYMENT_ARTIFACTS_STRING Header value
//                if ("DEPLOYMENT_ARTIFACTS_STRING".equals(localPart)) {
//                    try {
//                        JSONObject locationJson = new JSONObject(content);
//                        Optional<SupportedArtifactType> supportedArtifactType = SUPPORTED_ARTIFACT_TYPE_TYPES.stream()
//                                .filter(at -> locationJson.keySet().contains(at.artifactType))
//                                .findFirst();
//                        if (supportedArtifactType.isPresent()) {
//                            artifactType = supportedArtifactType.get();
//                            Set<String> strings = locationJson.getJSONObject(artifactType.artifactType).keySet();
//                            for (String key : strings) {
//                                if (key.endsWith(artifactType.fileType)) {
//                                    isoLocation = locationJson.getJSONObject(artifactType.artifactType).getString(key);
//                                    logger.info("Found a {} file available at {}", artifactType.fileType.toUpperCase(), isoLocation);
//                                    break;
//                                } else {
//                                    logger.warn("Found non matching file {} for artifact type {}", key, artifactType);
//                                }
//                            }
//                        }
//                    } catch (Exception e) {
//                        logger.info("Not able to find attached ISO DA...", e);
//                    }
//                }
//            }
//        }

        // we agreed in the IA knows the security group
        String securityGroup = "default";

        if (request.getVMSecurityGroup() != null && !request.getVMSecurityGroup().isEmpty()) {
            securityGroup = request.getVMSecurityGroup();
            if (!securityGroup.contains("default")) {
                securityGroup = "default," + securityGroup;
            }
        }
        logger.info("Received security groups {}", securityGroup);

        // Create OpenStack client
        OSClient<?> osClient = authenticate(
                request.getHypervisorEndpoint(),
                request.getHypervisorUserName(),
                request.getHypervisorUserPassword(),
                request.getHypervisorTenantID()
        );

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
            if (isoLocation != null && !isoLocation.isEmpty()) {
                logger.info("{} file is attached! Trying to create image with format '{}'", artifactType.fileType.toUpperCase(), artifactType.diskFormat.value());
                Payload<URL> payload = Payloads.create(new URL(isoLocation));

                String generatedImageId = "opentosca-" + request.getVMImageID().replaceAll("\\s", "") + "-" + System.currentTimeMillis();
                logger.info("Creating image with name '{}'", generatedImageId);

                uploadedImage = osClient.imagesV2().create(Builders.imageV2()
                        .name(generatedImageId)
                        .containerFormat(ContainerFormat.BARE)
                        .diskFormat(artifactType.diskFormat)
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
                .name("OTIA" + System.currentTimeMillis())
                .flavor(flavor)
                .image(image)
                .keypairName(request.getVMKeyPairName());

        for (String secGroup : securityGroup.split(",")) {
            String trim = secGroup.trim();
            if (!trim.isEmpty()) {
                serverCreateBuilder.addSecurityGroup(trim);
                logger.info("Added security group {}", trim);
            }
        }

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
                    logger.warn("Unable to allocate FloatingIP from pool {}. Trying next pool.", poolName, e);
                }
            }
        }

        // Have we been able to find a FloatingIP?
        if (floatingIp == null) {
            logger.error("Unable to find and allocate a FloatingIP. Machine will be started without floating IP and, therefore, might not be accessible from the outside.");
            // If not, we are setting the server's internal IP as floating IP,
            // so it is returned to the user, even if it might not be accesible.
            floatingIp = serversInternalIP;
        } else {
            // Assign Floating IP
            osClient.compute().floatingIps().addFloatingIP(server, floatingIp);
        }

        // Output Parameter 'VMInstanceId' (optional)
        // Do NOT delete the next line of code. Set "" as value if you want to
        // return nothing or an empty result!
        response.setVMInstanceID(server.getId());

        // Output Parameter 'VMIP' (optional)
        // Do NOT delete the next line of code. Set "" as value if you want to
        // return nothing or an empty result!
        response.setVMIP(floatingIp);

        SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
    }

    @PayloadRoot(namespace = OpenStackConstants.NAMESPACE_URI, localPart = "terminateVMRequest")
    public void terminateVM(@RequestPayload TerminateVMRequest request, MessageContext messageContext) {
        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());

        // Create OpenStack client
        OSClient<?> osClient = authenticate(
                request.getHypervisorEndpoint(),
                request.getHypervisorUserName(),
                request.getHypervisorUserPassword(),
                request.getHypervisorTenantID());
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
     * @param identificationEndpoint - The endpoint of the OpenStack Identity service
     * @param hypervisorUserName     - OpenStack user name
     * @param hypervisorUserPassword - OpenStack user password
     * @param projectId              - the id of the project to use
     * @return Authenticated OpenStack Client
     */
    private OSClient<?> authenticate(String identificationEndpoint, String hypervisorUserName, String hypervisorUserPassword,
                                     String projectId) {
        Config config = newConfig().withSSLVerificationDisabled();

        // Authenticate with OpenStack
        String endpoint = identificationEndpoint.endsWith("/v3") ? "" : ":5000/v3";
        if (identificationEndpoint.startsWith("http")) {
            endpoint = identificationEndpoint + endpoint;
        } else {
            endpoint = "http://" + identificationEndpoint + endpoint;
        }

        // v3 auth
        logger.info("Connecting to \"{}\"...", endpoint);
        V3 creds = OSFactory.builderV3()
                .withConfig(config)
                .perspective(Facing.PUBLIC)
                .endpoint(endpoint)
                .credentials(hypervisorUserName, hypervisorUserPassword, Identifier.byName("Default"))
                .scopeToProject(Identifier.byId(projectId));
        try {
            return creds.authenticate();
        } catch (Exception e) {
            logger.error("Error while authenticating at {}", endpoint, e);
        }

        return null;
    }

    private record SupportedArtifactType(String artifactType, String fileType,
                                         DiskFormat diskFormat) {
    }
}
