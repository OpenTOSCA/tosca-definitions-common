package org.opentosca.artifacttemplates.dockercontainer;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
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

@Endpoint
public class DockerContainerManagementInterfaceEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagementInterfaceEndpoint.class);

    @PayloadRoot(namespace = DockerContainerConstants.NAMESPACE_URI, localPart = "runScriptRequest")
    public void runScript(@RequestPayload RunScriptRequest request, MessageContext messageContext) {
        LOG.info("Received runScript request!");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);
        InvokeResponse invokeResponse = new InvokeResponse();
        invokeResponse.setMessageID(openToscaHeaders.messageId());

        try {
            DockerContainer container = new DockerContainer(request.getDockerEngineURL(), request.getDockerEngineCertificate(), request.getContainerID());
            container.awaitAvailability();

            container.ensurePackage("sudo");

            String command = container.replaceHome(request.getScript());
            String result = container.execCommand(command);
            invokeResponse.setScriptResult(result);
        } catch (InterruptedException e) {
            LOG.error("Could not execute script", e);
            invokeResponse.setError("Could not execute script: " + e.getMessage());
        }

        SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
    }

    @PayloadRoot(namespace = DockerContainerConstants.NAMESPACE_URI, localPart = "runScriptRequest")
    public void transferFile(@RequestPayload TransferFileRequest request, MessageContext messageContext) {
        LOG.info("Received transferFile request!");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);
        InvokeResponse invokeResponse = new InvokeResponse();
        invokeResponse.setMessageID(openToscaHeaders.messageId());

        DockerContainer container = new DockerContainer(request.getDockerEngineURL(), request.getDockerEngineCertificate(), request.getContainerID());
        container.awaitAvailability();

        // Transform sourceURLorLocalAbsolutePath to URL
        URL url;
        try {
            // Check if the string is a URL right away?
            url = new URL(request.getSourceURLorLocalPath());
        } catch (Exception e) {
            // It's not a URL
            // Check if string is a local path
            File file = new File(request.getSourceURLorLocalPath());
            if (file.exists()) {
                try {
                    String target = container.replaceHome(request.getTargetAbsolutePath());
                    container.uploadFile(file.toString(), target);
                    container.convertToUnix(target);
                } catch (Exception ex) {
                    LOG.error("Could not transfer file", ex);
                    invokeResponse.setError("Could not transfer file: " + ex.getMessage());
                }
            } else {
                String message = "TRANSFER_FAILED: File " + request.getSourceURLorLocalPath() + " is no valid URL and does not exist on the local file system.";
                LOG.error(message);
                invokeResponse.setError("Could not transfer file: " + message);
            }
            SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
            return;
        }

        // Opens stream and uploads file
        HttpURLConnection httpConnection = null;
        InputStream inputStream = null;
        try {
            // If there is no output stream a HTTP GET is done by default
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestProperty("Accept", "application/octet-stream");
            inputStream = httpConnection.getInputStream();

            String target = container.replaceHome(request.getTargetAbsolutePath());
            int start = target.lastIndexOf('/') + 1;
            String filename = target.substring(start);

            Path tempDirectory = Files.createTempDirectory(filename);
            File tempFile = new File(tempDirectory.toString(), filename);
            FileUtils.copyInputStreamToFile(inputStream, tempFile);
            LOG.info("Temp file {} exists: {}", tempFile, tempFile.exists());

            container.uploadFile(tempFile.toString(), target);
            container.convertToUnix(target);

            FileUtils.deleteDirectory(tempDirectory.toFile());
            LOG.info("Deleting temp file was successful!");

            invokeResponse.setTransferResult("successful");
        } catch (Exception e) {
            LOG.error("Could not transfer file", e);
            invokeResponse.setError("Could not transfer file: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            } catch (Exception e) {
                LOG.error("Could not close resources", e);
                invokeResponse.setError("Could not close resources: " + e.getMessage());
            }
        }

        SoapUtil.sendSoapResponse(invokeResponse, InvokeResponse.class, openToscaHeaders.replyTo());
    }
}
