package org.opentosca.nodetypeimplementations;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.eclipse.winery.generators.ia.jaxws.AbstractService;
import org.eclipse.winery.generators.ia.jaxws.Headers;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService
public class org_opentosca_nodetypes_KVM_QEMU_VM__OperatingSystemInterface extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(
            org_opentosca_nodetypes_KVM_QEMU_VM__OperatingSystemInterface.class
    );

    @WebMethod
    @SOAPBinding
    @Oneway
    public void transferFile(
            @WebParam(name = "VMIP", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMIP,
            @WebParam(name = "VMUserName", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserName,
            @WebParam(name = "VMUserPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserPassword,
            @WebParam(name = "SourceURLorLocalPath", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String SourcePath,
            @WebParam(name = "TargetAbsolutePath", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String TargetPath
    ) {
        logger.info("BEGINN - transferFile()");
        logger.info("VMIP: {}", VMIP);
        logger.info("VMUserName: {}", VMUserName);
        logger.info("VMUserPassword: {}", VMUserPassword);
        logger.info("SourcePath: {}", SourcePath);
        logger.info("TargetPath: {}", TargetPath);
        Headers.asMap(ctx).forEach((k, v) -> logger.info("{}: {}", k, v));

        // Create SSH template
        SshTemplate ssh = new SshTemplate(VMIP, VMUserName, VMUserPassword);

        String result = "SUCCESS";

        // Prepare file for upload
        File file = new File(SourcePath);
        try {
            // If file does not exist, assume it is an URL
            if (!file.exists()) {
                file = File.createTempFile("temp", ".tmp");
                file.deleteOnExit();
                URL source = new URL(SourcePath);
                FileUtils.copyURLToFile(source, file);
            }
        } catch (Exception e) {
            logger.error("Could not download file: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }

        // Resolve absolute user home directory
        if (TargetPath.startsWith("~")) {
            String pwd = ssh.executeCommand("pwd");
            TargetPath = TargetPath.replaceFirst("~", pwd);
            logger.info("Replaced \"~/\" with user home at \"{}\"", TargetPath);
        }

        try {
            ssh.transferFile(file.getAbsolutePath(), TargetPath);
        } catch (Exception e) {
            result = "ERROR";
        }

        final HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("TransferFileResult", result);
        sendResponse(returnParameters);

        logger.info("TransferFileResult: {}", result);
        logger.info("END - transferFile()");
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void runScript(
            @WebParam(name = "VMIP", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMIP,
            @WebParam(name = "VMUserName", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserName,
            @WebParam(name = "VMUserPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserPassword,
            @WebParam(name = "Script", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String Script
    ) {
        logger.info("BEGINN - runScript()");
        logger.info("VMIP: {}", VMIP);
        logger.info("VMUserName: {}", VMUserName);
        logger.info("VMUserPassword: {}", VMUserPassword);
        logger.info("Script: {}", Script);
        Headers.asMap(ctx).forEach((k, v) -> logger.info("{}: {}", k, v));

        // Create SSH template
        SshTemplate ssh = new SshTemplate(VMIP, VMUserName, VMUserPassword);

        String result;
        try {
            result = ssh.executeCommand(Script);
        } catch (Exception e) {
            result = "ERROR";
        }

        final HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("ScriptResult", result);
        sendResponse(returnParameters);

        logger.info("ScriptResult: {}", result);
        logger.info("END - runScript()");
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void waitForAvailability(
            @WebParam(name = "VMIP", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMIP,
            @WebParam(name = "VMUserName", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserName,
            @WebParam(name = "VMUserPassword", targetNamespace = "http://nodetypeimplementations.opentosca.org/") String VMUserPassword
    ) {
        logger.info("BEGINN - waitForAvailability()");
        logger.info("VMIP: {}", VMIP);
        logger.info("VMUserName: {}", VMUserName);
        logger.info("VMUserPassword: {}", VMUserPassword);
        Headers.asMap(ctx).forEach((k, v) -> logger.info("{}: {}", k, v));

        // Create SSH template
        SshTemplate ssh = new SshTemplate(VMIP, VMUserName, VMUserPassword);

        String result = "SUCCESS";
        if (ssh.canConnect()) {
            logger.info("Successfully connected to virtual machine on <{}>", VMIP);
        } else {
            logger.error("Could not connect to virtual machine on <{}>", VMIP);
            result = "ERROR";
        }
        if (ssh.canLogin()) {
            logger.info("Successfully logged-in to virtual machine on <{}>", VMIP);
        } else {
            logger.error("Could not log-in to virtual machine on <{}>", VMIP);
            result = "ERROR";
        }

        final HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("WaitResult", result);
        sendResponse(returnParameters);

        logger.info("WaitResult: {}", result);
        logger.info("END - waitForAvailability()");
    }
}
