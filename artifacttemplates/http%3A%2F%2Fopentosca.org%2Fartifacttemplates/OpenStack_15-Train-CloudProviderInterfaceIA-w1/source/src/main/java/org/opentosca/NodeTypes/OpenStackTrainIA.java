package org.opentosca.NodeTypes;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.WebServiceContext;

import com.google.gson.Gson;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.json.JSONObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import static org.openstack4j.core.transport.Config.newConfig;

@WebService(targetNamespace = "http://implementationartifacts.opentosca.org/")
public class OpenStackTrainIA extends AbstractIAService {

    private final static Logger logger = LoggerFactory.getLogger(OpenStackTrainIA.class);

    private final static List<SupportedArtifactType> SUPPORTED_ARTIFACT_TYPE_TYPES = Arrays.asList(
            new SupportedArtifactType("{http://opentosca.org/artifacttypes}ISO", "iso", DiskFormat.RAW),
            new SupportedArtifactType("{http://opentosca.org/artifacttypes}CloudImage", "img", DiskFormat.QCOW2)
    );

    @Resource
    private WebServiceContext context;

    @WebMethod
    @SOAPBinding
    @Oneway
    public void createVM(
            @WebParam(name = "HypervisorEndpoint", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackIdentityEndpoint,
            @WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackUserName,
            @WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackUserPassword,
            @WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackProjectId,
            @WebParam(name = "VMType", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmType,
            @WebParam(name = "VMImageID", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmImageId,
            @WebParam(name = "VMUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmUserName,
            @WebParam(name = "VMUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmUserPassword,
            @WebParam(name = "VMPrivateKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmPrivateKey,
            @WebParam(name = "VMPublicKey", targetNamespace = "http://implementationartifacts.opentosca.org/") String mvPublicKey,
            @WebParam(name = "VMKeyPairName", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmKeyPairName,
            @WebParam(name = "VMSecurityGroup", targetNamespace = "http://implementationartifacts.opentosca.org/") String vmSecurityGroup) {
        OSFactory.enableHttpLoggingFilter(true);
        final HashMap<String, String> returnParameters = new HashMap<>();
        SupportedArtifactType artifactType = SUPPORTED_ARTIFACT_TYPE_TYPES.get(1);
        Gson gson = new Gson();

        String isoLocation = null;
//        isoLocation = "file:///C:/Users/lharz/Downloads/bionic-server-cloudimg-amd64.img";

        // Extract message
        WrappedMessageContext wrappedContext = (WrappedMessageContext) context.getMessageContext();

        Message message = wrappedContext.getWrappedMessage();

        // Extract headers from message
        List<Header> headers = CastUtils.cast((List<?>) message.get(Header.HEADER_LIST));

        for (Header header : headers) {
            Object headerObject = header.getObject();
            logger.info("Found header: '{}' and content '{}'", header.getName(), headerObject);

            // Unmarshall to org.w3c.dom.Node
            if (headerObject instanceof Node) {
                Node node = (Node) headerObject;
                String localPart = header.getName().getLocalPart();
                String content = node.getTextContent();

                // Extract DEPLOYMENT_ARTIFACTS_STRING Header value
                if ("DEPLOYMENT_ARTIFACTS_STRING".equals(localPart)) {
                    try {
                        JSONObject locationJson = new JSONObject(content);
                        Optional<SupportedArtifactType> supportedArtifactType = SUPPORTED_ARTIFACT_TYPE_TYPES.stream()
                                .filter(at -> locationJson.keySet().contains(at.artifactType))
                                .findFirst();
                        if (supportedArtifactType.isPresent()) {
                            artifactType = supportedArtifactType.get();
                            Set<String> strings = locationJson.getJSONObject(artifactType.artifactType).keySet();
                            for (String key : strings) {
                                if (key.endsWith(artifactType.fileType)) {
                                    isoLocation = locationJson.getJSONObject(artifactType.artifactType).getString(key);
                                    logger.info("Found a {} file available at {}", artifactType.fileType.toUpperCase(), isoLocation);
                                    break;
                                } else {
                                    logger.warn("Found non matching file {} for artifact type {}", key, artifactType);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.info("Not able to find attached ISO DA...", e);
                    }
                }
            }
        }

        // we agreed in the IA is knowing the security group
        String securityGroup = "default";

        if (vmSecurityGroup != null && !vmSecurityGroup.isEmpty()) {
            securityGroup = vmSecurityGroup;
            if (!securityGroup.contains("default")) {
                securityGroup = "default," + securityGroup;
            }
        }
        logger.info("Received security groups {}", securityGroup);

        // Create OpenStack client
        OSClient<?> osClient = authenticate(openStackIdentityEndpoint, openStackUserName, openStackUserPassword, openStackProjectId);

        if (osClient == null) {
            returnParameters.put("Error", "Could not connect to OpenStack Instance at " + openStackIdentityEndpoint);
            sendResponse(returnParameters);
            logger.error("Could not create connection...");
            return;
        } else {
            logger.info("Successfully authenticated at {}", openStackIdentityEndpoint);
        }

        org.openstack4j.model.image.v2.Image uploadedImage = null;
        try {
            if (isoLocation != null && !isoLocation.isEmpty()) {
                logger.info("{} file is attached! Trying to create image with format '{}'", artifactType.fileType.toUpperCase(), artifactType.diskFormat.value());
                Payload<URL> payload = Payloads.create(new URL(isoLocation));

                String generatedImageId = "opentosca-" + vmImageId.replaceAll("\\s", "") + "-" + System.currentTimeMillis();
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
            returnParameters.put("error", "could not upload image");

            sendResponse(returnParameters);
            return;
        }

        // Get Flavor based on Type String
        List<? extends Flavor> flavours = osClient.compute().flavors().list();
        Flavor flavor = null;
        for (Flavor f : flavours) {
            if (f.getId().equals(vmType) || f.getName().equals(vmType)) {
                flavor = f;
                break;
            }
        }
        if (flavor == null) {
            returnParameters.put("Error", "Cannot find flavor for input " + vmType);
            logger.error("Cannot find flavor for input {}", vmType);
            sendResponse(returnParameters);
            throw new RuntimeException("Cannot find flavor for input " + vmType);
        }
        logger.info("Found flavour {}", flavor.getName());

        String[] imageIdParts = vmImageId.split("-");
        // Get Flavor based on Type String
        List<? extends Image> images = osClient.compute().images().list();
        Image image;

        Optional<? extends Image> optionalImage;

        if (uploadedImage == null) {
            optionalImage = images.stream()
                    .filter(img -> {
                        final String imageName = img.getName().toLowerCase();
                        return imageName.equals(vmImageId.toLowerCase()) || imageName.startsWith(vmImageId)
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
            returnParameters.put("Error", "Cannot find image for input " + vmImageId);
            sendResponse(returnParameters);
            throw new RuntimeException("Cannot find image for input " + vmImageId);
        }

        // Build the server
        ServerCreateBuilder serverCreateBuilder = Builders.server()
                .name("OTIA" + System.currentTimeMillis())
                .flavor(flavor)
                .image(image)
                .keypairName(vmKeyPairName);

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

        // Retrying for some minutes for OpenStack to setup the server
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
        returnParameters.put("VMInstanceID", server.getId());

        // Output Parameter 'VMIP' (optional)
        // Do NOT delete the next line of code. Set "" as value if you want to
        // return nothing or an empty result!
        returnParameters.put("VMIP", floatingIp);

        sendResponse(returnParameters);
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void terminateVM(
            @WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackIdentityEndpoint,
            @WebParam(name = "HypervisorUserName", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackUserName,
            @WebParam(name = "HypervisorUserPassword", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackUserPassword,
            @WebParam(name = "HypervisorTenantID", targetNamespace = "http://implementationartifacts.opentosca.org/") String openStackProjectId,
            @WebParam(name = "VMInstanceID", targetNamespace = "http://implementationartifacts.opentosca.org/") String VMInstanceId) {
        final HashMap<String, String> returnParameters = new HashMap<>();

        // Create OpenStack client
        OSClient<?> osClient = authenticate(openStackIdentityEndpoint, openStackUserName, openStackUserPassword, openStackProjectId);
        logger.info("Successfully authenticated at {}", openStackIdentityEndpoint);

        if (osClient == null) {
            returnParameters.put("Error", "Could not connect to OpenStack Instance at " + openStackIdentityEndpoint);
            sendResponse(returnParameters);
            logger.error("Could not create connection...");
            return;
        }

        // Get the server object
        Server server = null;
        for (Server s : osClient.compute().servers().list()) {
            // Search by ID and Name (this is more than advertised in the
            // interface, i.e. publicDNSorInstanceId)
            if (s.getId().equals(VMInstanceId) || s.getName().equals(VMInstanceId)) {
                server = s;
                break;
            }

            // Search by IP
            for (List<? extends Address> addressesOfNetwork : s.getAddresses().getAddresses().values()) {
                for (Address address : addressesOfNetwork) {
                    if (address.getAddr().equals(VMInstanceId)) {
                        server = s;
                        break;
                    }
                }
            }
        }

        if (server == null) {
            returnParameters.put("Error", "Could not find server with ID \"" + VMInstanceId + "\"");
            logger.info("Could not find server with ID \"{}\"", VMInstanceId);
        } else {
            // Shutdown and delete this server
            osClient.compute().servers().delete(server.getId());
            returnParameters.put("Result", "true");
            logger.info("Successfully terminated server with ID \"{}}\"", VMInstanceId);
        }

        sendResponse(returnParameters);
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

    private static class SupportedArtifactType {

        final String artifactType;
        final String fileType;
        final DiskFormat diskFormat;

        public SupportedArtifactType(String artifactType, String fileType, DiskFormat diskFormat) {
            this.artifactType = artifactType;
            this.fileType = fileType;
            this.diskFormat = diskFormat;
        }
    }
}
