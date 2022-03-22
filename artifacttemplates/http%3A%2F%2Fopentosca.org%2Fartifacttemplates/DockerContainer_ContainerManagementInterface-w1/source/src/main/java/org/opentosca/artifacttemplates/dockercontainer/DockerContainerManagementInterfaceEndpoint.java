package org.opentosca.artifacttemplates.dockercontainer;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import static org.opentosca.artifacttemplates.dockercontainer.FileHandler.downloadFile;
import static org.opentosca.artifacttemplates.dockercontainer.FileHandler.getFile;
import static org.opentosca.artifacttemplates.dockercontainer.FileHandler.getUrl;

@Endpoint
public class DockerContainerManagementInterfaceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagementInterfaceEndpoint.class);

    @PayloadRoot(namespace = DockerContainerConstants.NAMESPACE_URI, localPart = "runScriptRequest")
    public void runScript(@RequestPayload RunScriptRequest request, MessageContext messageContext) {
        LOG.info("RunScript request received!");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);
        InvokeResponse invokeResponse = new InvokeResponse();
        invokeResponse.setMessageID(openToscaHeaders.messageId());

        try {
            DockerContainer container = new DockerContainer(request.getDockerEngineURL(), request.getDockerEngineCertificate(), request.getContainerID());
            container.awaitAvailability();
            container.ensurePackage("sudo");
            String command = container.replaceHome(request.getScript());
            String result = container.execCommand(command);
            invokeResponse.setScriptResult(SoapUtil.encode(result));
            LOG.info("RunScript request successful");
        } catch (InterruptedException e) {
            LOG.error("Could not execute script", e);
            invokeResponse.setError("Could not execute script: " + e.getMessage());
        }

        SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
    }

    @PayloadRoot(namespace = DockerContainerConstants.NAMESPACE_URI, localPart = "transferFileRequest")
    public void transferFile(@RequestPayload TransferFileRequest request, MessageContext messageContext) {
        LOG.info("TransferFile request received");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);
        InvokeResponse invokeResponse = new InvokeResponse();
        invokeResponse.setMessageID(openToscaHeaders.messageId());

        try {
            // Connect to container
            DockerContainer container = new DockerContainer(
                    request.getDockerEngineURL(),
                    request.getDockerEngineCertificate(),
                    request.getContainerID()
            );
            container.awaitAvailability();

            // Source and target paths
            String source = null;
            String target = container.replaceHome(request.getTargetAbsolutePath());

            // Use file from URL as source if valid URL
            Path tempDirectory = null;
            URL url = getUrl(request.getSourceURLorLocalPath());

            if (url != null) {
                LOG.info("Transferring file from URL '{}' to container", request.getSourceURLorLocalPath());
                String filename = target.substring(target.lastIndexOf('/') + 1);
                tempDirectory = Files.createTempDirectory(filename);
                source = downloadFile(url, tempDirectory.toString(), filename);
            }

            // Use local file as source if file exists
            File file = getFile(request.getSourceURLorLocalPath());
            if (file != null) {
                LOG.info("Transferring local file '{}' to container", request.getSourceURLorLocalPath());
                source = file.toString();
            }

            // Abort if no source
            if (source == null) {
                String message = "File " + request.getSourceURLorLocalPath() + " is no valid URL and does not exist on the local file system.";
                LOG.error(message);
                invokeResponse.setError("Could not transfer file: " + message);
                SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
                return;
            }

            // Upload source to target on container
            container.uploadFile(source, target);

            // Convert target on container to unix
            container.convertToUnix(target);

            // Clean up
            if (tempDirectory != null) {
                FileUtils.deleteDirectory(tempDirectory.toFile());
                LOG.info("Deleted temporary directory successful");
            }

            // Success
            invokeResponse.setTransferResult("successful");
            LOG.info("TransferFile request successful");
        } catch (Exception e) {
            LOG.error("Could not transfer file...", e);
            invokeResponse.setError("Could not transfer file: " + e.getMessage());
        }

        // Send response
        SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
    }
}
