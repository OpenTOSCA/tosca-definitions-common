package org.opentosca.nodetypeimplementations;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;
import org.eclipse.winery.generators.ia.jaxws.Headers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

@WebService
public class org_opentosca_nodetypes_KVM_QEMU_Hypervisor__CloudProviderInterface extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(
            org_opentosca_nodetypes_KVM_QEMU_Hypervisor__CloudProviderInterface.class
    );

    @WebMethod
    @SOAPBinding
    @Oneway
    public void createVM(
            @WebParam(name = "HypervisorEndpoint", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorEndpoint,
            @WebParam(name = "HypervisorUser", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorUser,
            @WebParam(name = "HypervisorPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorPassword,
            @WebParam(name = "HypervisorBridgeInterface", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorBridgeInterface,
            @WebParam(name = "VMUserName", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserName,
            @WebParam(name = "VMUserPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserPassword,
            @WebParam(name = "VMDiskSize", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMDiskSize,
            @WebParam(name = "VMVCPUS", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMVCPUS,
            @WebParam(name = "VMRAM", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMRAM,
            @WebParam(name = "VMOSType", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMOSType,
            @WebParam(name = "VMOSVariant", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMOSVariant
    ) {
        logger.info("BEGIN - createVM()");
        logger.info("HypervisorEndpoint: {}", HypervisorEndpoint);
        logger.info("HypervisorUser: {}", HypervisorUser);
        logger.info("HypervisorPassword: {}", HypervisorPassword);
        logger.info("HypervisorBridgeInterface: {}", HypervisorBridgeInterface);
        logger.info("VMUserName: {}", VMUserName);
        logger.info("VMUserPassword: {}", VMUserPassword);
        logger.info("VMDiskSize: {}", VMDiskSize);
        logger.info("VMVCPUS: {}", VMVCPUS);
        logger.info("VMRAM: {}", VMRAM);
        logger.info("VMOSType: {}", VMOSType);
        logger.info("VMOSVariant: {}", VMOSVariant);
        Headers.asMap(ctx).forEach((k, v) -> logger.info("{}: {}", k, v));

        // Create SSH template
        SshTemplate ssh = new SshTemplate(HypervisorEndpoint, HypervisorUser, HypervisorPassword);

        // Generate new VM instance ID
        String VMInstanceID = UUID.randomUUID().toString();

        // Enable "arpwatch" to scrape IP address of virtual machine
        ssh.executeCommand("sudo arpwatch -i " + HypervisorBridgeInterface + "; sudo arpwatch -i eth0;");

        // Move createVM.sh to target directory
        String targetPath = "/home/" + HypervisorUser + "/" + VMInstanceID;
        ssh.executeCommand("mkdir " + targetPath);
        ssh.transferFile(getFileFromClasspath().getAbsolutePath(), targetPath);
        ssh.executeCommand("dos2unix " + targetPath + "/createVM.sh");

        // Build script parameters
        String params = "" +
                "VMID=\"" + VMInstanceID + "\" " +
                "VMDiskSize=\"" + VMDiskSize + "\" " +
                "VMUserName=\"" + VMUserName + "\" " +
                "VMUserPassword=\"" + VMUserPassword + "\" " +
                "VMVCPUS=\"" + VMVCPUS + "\" " +
                "VMRAM=\"" + VMRAM + "\" " +
                "VMOSType=\"" + VMOSType + "\" " +
                "VMOSVariant=\"" + VMOSVariant + "\" " +
                "HypervisorBridgeInterface=\"" + HypervisorBridgeInterface + "\"";
        logger.info("Parameters: {}", params);

        // Execute createVM.sh
        ssh.executeCommand("cd " + targetPath + "; " + params + " sh createVM.sh; sleep 5");

        // Grep MAC address
        String VMMAC = ssh.executeCommand("virsh dumpxml " + VMInstanceID + " | grep \"mac address\" | sed \"s/.*'\\(.*\\)'.*/\\1/g\"");
        logger.info("VMMAC: {}", VMMAC);

        // Grep IP address
        String VMIP = getIpAddress(ssh, VMMAC);
        logger.info("VMIP: {}", VMIP);

        // Check if we can login to the new virtual machine
        SshTemplate vm = new SshTemplate(VMIP, VMUserName, VMUserPassword);
        if (vm.canConnect()) {
            logger.info("Successfully connected to virtual machine on <{}>", VMIP);
        }
        if (vm.canLogin()) {
            logger.info("Successfully logged-in to virtual machine on <{}>", VMIP);
        }

        // Collect return parameters
        final HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("VMIP", VMIP);
        returnParameters.put("VMInstanceID", VMInstanceID);
        returnParameters.put("VMMAC", VMMAC);
        returnParameters.put("CreateResult", "SUCCESS");
        sendResponse(returnParameters);

        logger.info("VMIP: {}", VMIP);
        logger.info("VMInstanceID: {}", VMInstanceID);
        logger.info("VMMAC: {}", VMMAC);
        logger.info("END - createVM()");
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void terminateVM(
            @WebParam(name = "HypervisorEndpoint", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorEndpoint,
            @WebParam(name = "HypervisorUser", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorUser,
            @WebParam(name = "HypervisorPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String HypervisorPassword,
            @WebParam(name = "VMInstanceID", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMInstanceID
    ) {
        logger.info("BEGIN - terminateVM()");
        logger.info("HypervisorEndpoint: {}", HypervisorEndpoint);
        logger.info("HypervisorUser: {}", HypervisorUser);
        logger.info("HypervisorPassword: {}", HypervisorPassword);
        logger.info("VMInstanceID: {}", VMInstanceID);
        Headers.asMap(ctx).forEach((k, v) -> logger.info("{}: {}", k, v));

        // Create SSH template
        SshTemplate ssh = new SshTemplate(HypervisorEndpoint, HypervisorUser, HypervisorPassword);

        // Destroy instance
        ssh.executeCommand("virsh destroy " + VMInstanceID + " && virsh undefine " + VMInstanceID);

        // Collect return parameters
        HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("TerminateResult", "SUCCESS");
        sendResponse(returnParameters);

        logger.info("END - terminateVM()");
    }

    private File getFileFromClasspath() {
        ClassPathResource resource = new ClassPathResource("createVM.sh");
        try {
            return resource.getFile();
        } catch (Exception e) {
            logger.error("Could not load createVM.sh file from classpath");
            throw new IllegalStateException("Could not load createVM.sh file from classpath");
        }
    }

    private String getIpAddress(SshTemplate ssh, String VMMAC) {
        RetryCommand<String> retry = new RetryCommand<>();
        String command1 = "grep \"arpwatch: new station.*" + VMMAC + ".*\" /var/log/syslog | tail -1 | sed \"s/.*arpwatch: new station \\(.*\\)/\\1/g\" | sed 's/\\s.*$//'";
        String command2 = "grep \"arpwatch: changed ethernet address.*" + VMMAC + ".*\" /var/log/syslog | tail -1 | sed \"s/.*arpwatch: changed ethernet address \\(.*\\)/\\1/g\" | sed 's/\\s.*$//'";
        return retry.run(() -> {
            logger.info("Determine IP address grepping for \"arpwatch: new station\"");
            String result = ssh.executeCommand(command1);
            if (result.isEmpty()) {
                logger.info("IP address not found, grepping for \"arpwatch: changed ethernet address\"");
                result = ssh.executeCommand(command2);
                if (result.isEmpty()) {
                    logger.info("Could not determine IP address; wait for next attempt...");
                    throw new RuntimeException();
                }
            }
            return result;
        });
    }
}
