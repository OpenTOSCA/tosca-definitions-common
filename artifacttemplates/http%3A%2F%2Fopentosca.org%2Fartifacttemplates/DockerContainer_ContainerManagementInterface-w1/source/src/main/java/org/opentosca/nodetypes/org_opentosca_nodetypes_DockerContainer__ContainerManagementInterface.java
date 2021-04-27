package org.opentosca.nodetypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.apache.commons.io.FileUtils;

@WebService
public class org_opentosca_nodetypes_DockerContainer__ContainerManagementInterface extends AbstractIAService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void runScript(
            @WebParam(name = "DockerEngineURL", targetNamespace = "http://nodetypes.opentosca.org/") final String dockerEngineURL,
            @WebParam(name = "DockerEngineCertificate", targetNamespace = "http://nodetypes.opentosca.org/") final String dockerEngineCertificate,
            @WebParam(name = "ContainerID", targetNamespace = "http://nodetypes.opentosca.org/") String containerID,
            @WebParam(name = "Script", targetNamespace = "http://nodetypes.opentosca.org/") String script) {
        final HashMap<String, String> returnParameters = new HashMap<>();

        System.out.println("Received ContainerID: '" + containerID + "'");

        waitForAvailability(dockerEngineURL, dockerEngineCertificate, containerID);

        if (!hasSudo(dockerEngineURL, dockerEngineCertificate, containerID)) {
            installSudo(dockerEngineURL, dockerEngineCertificate, containerID);
        }

        try {
            String augmentedScript = replaceHome(dockerEngineURL, dockerEngineCertificate, containerID, script);
            String res = executeCommand(dockerEngineURL, dockerEngineCertificate, containerID, augmentedScript);
            returnParameters.put("ScriptResult", res);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendResponse(returnParameters);
    }

    private void installSudo(String dockerEngineURL, String dockerEngineCertificate, String containerId) {
        try {
            executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "apt update && apt -yq install sudo");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean hasSudo(String dockerEngineURL, String dockerEngineCertificate, String containerId) {
        try {
            System.out.println("Checking if sudo is installed");
            String result = executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "sudo");
            boolean hasSudo = !result.isEmpty() && !result.contains("not found");
            System.out.println("Sudo check result: " + hasSudo);
            return hasSudo;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void waitForAvailability(String dockerEngineURL, String dockerEngineCertificate, String containerId) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 25000;
        while (System.currentTimeMillis() < endTime) {
            try {
                executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "pwd");
                // if we can execute pwd without issues ssh is up!
                break;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    String executeCommand(String dockerEngineUrl, String dockerEngineCertificate, String containerId, String script) throws InterruptedException {
        DockerClient dockerClient = getClient(dockerEngineUrl, dockerEngineCertificate);

        System.out.println("Executing shell command: " + script);

        try (OutputStream output = getStringOutputStream()) {
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withTty(true)
                    .withAttachStdout(true)
                    .withCmd("/bin/bash", "-c", script)
                    .exec();
            dockerClient.execStartCmd(execCreateCmdResponse.getId())
                    .withTty(true)
                    .withDetach(false)
                    .exec(new ExecStartResultCallback(output, System.err))
                    .awaitCompletion();

            String log = output.toString();
            System.out.println("Execution log:");
            System.out.println(log);

            return log;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private OutputStream getStringOutputStream() {
        return new OutputStream() {
            private StringBuilder string = new StringBuilder();

            @Override
            public void write(int x) throws IOException {
                this.string.append((char) x);
            }

            public String toString() {
                return this.string.toString();
            }
        };
    }

    private DockerClient getClient(String dockerEngineUrl, String dockerEngineCertificate) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerEngineUrl)
                .withDockerTlsVerify(false)
                .build();
        return DockerClientBuilder.getInstance(config).build();
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void transferFile(
            @WebParam(name = "DockerEngineURL", targetNamespace = "http://nodetypes.opentosca.org/") final String dockerEngineURL,
            @WebParam(name = "DockerEngineCertificate", targetNamespace = "http://nodetypes.opentosca.org/") final String dockerEngineCertificate,
            @WebParam(name = "ContainerID", targetNamespace = "http://nodetypes.opentosca.org/") String containerId,
            @WebParam(name = "TargetAbsolutePath", targetNamespace = "http://nodetypes.opentosca.org/") String targetAbsolutePath,
            @WebParam(name = "SourceURLorLocalPath", targetNamespace = "http://nodetypes.opentosca.org/") String sourceURLorLocalPath) {
        // This HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        waitForAvailability(dockerEngineURL, dockerEngineCertificate, containerId);

        if (targetAbsolutePath.startsWith("~")) {
            targetAbsolutePath = replaceHome(dockerEngineURL, dockerEngineCertificate, containerId, targetAbsolutePath, false);
        }

        // Transform sourceURLorLocalAbsolutePath to URL
        URL url;
        try {
            // Check if the string is a URL right away?
            url = new URL(sourceURLorLocalPath);
        } catch (Exception e) {
            // It's not a URL
            // Check if string is a local path
            File file = new File(sourceURLorLocalPath);
            if (file.exists()) {
                try {
                    uploadFile(dockerEngineURL, dockerEngineCertificate, containerId, file.toString(), targetAbsolutePath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    returnParameters.put("TransferResult", "TRANSFER_FAILED: " + ex);
                }
            } else {
                // FAILED: Return async message
                returnParameters.put("TransferResult", "TRANSFER_FAILED: File " + sourceURLorLocalPath
                        + " is no valid URL and does not exist on the local file system.");
                sendResponse(returnParameters);
            }
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

            int start = targetAbsolutePath.lastIndexOf('/') + 1;
            String filename = targetAbsolutePath.substring(start);

            Path tempDirectory = Files.createTempDirectory(filename);
            File tempFile = new File(tempDirectory.toString(), filename);

            FileUtils.copyInputStreamToFile(inputStream, tempFile);

            System.out.println("Temp file '" + tempFile.toString() + "' exists: " + tempFile.exists());

            uploadFile(dockerEngineURL, dockerEngineCertificate, containerId, tempFile.toString(), targetAbsolutePath);

            FileUtils.deleteDirectory(tempDirectory.toFile());
            System.out.println("Deleting temp file was successful!");

            returnParameters.put("TransferResult", "successful");
        } catch (Exception e) {
            e.printStackTrace();
            returnParameters.put("TransferResult", "TRANSFER_FAILED: " + e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (httpConnection != null) {
                    httpConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Returning a parameter is required so that we can wait asynchronously
        // in the process.
        sendResponse(returnParameters);
    }

    private String replaceHome(String dockerEngineURL, String dockerEngineCertificate, String host, String commandString) {
        return replaceHome(dockerEngineURL, dockerEngineCertificate, host, commandString, true);
    }

    private String replaceHome(String dockerEngineURL, String dockerEngineCertificate, String host, String commandString, boolean all) {
        if (commandString.contains("~")) {
            try {
                String pwd = executeCommand(dockerEngineURL, dockerEngineCertificate, host, "pwd").trim();
                System.out.println("Replaced ~ with user home ('" + pwd + "'): '" + commandString + "'");
                return all ? commandString.replaceAll("~", pwd) : commandString.replaceFirst("~", pwd);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return commandString;
    }

    private void uploadFile(String dockerEngineURL, String dockerEngineCertificate, String containerId, String uploadFile, String target) throws InterruptedException, UnsupportedEncodingException {
        DockerClient dockerClient = getClient(dockerEngineURL, dockerEngineCertificate);

        System.out.println("Uploading file '" + uploadFile + "' to '" + target + "'...");

        int end = target.lastIndexOf('/');
        String folders = target.substring(0, end);
        executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "mkdir -p " + folders);

        dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath(URLEncoder.encode(folders, "UTF-8"))
                .withHostResource(uploadFile)
                .exec();

        System.out.println("Successfully uploaded file!");
    }
}
