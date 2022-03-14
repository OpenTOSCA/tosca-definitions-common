package org.opentosca.artifacttemplates.dockercontainer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerClientHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DockerClientHandler.class);

    static DefaultDockerClientConfig getConfig(String DockerEngineURL, String DockerEngineCertificate) {
        DefaultDockerClientConfig.Builder config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(DockerEngineURL)
                .withApiVersion("1.21");

        if (DockerEngineCertificate == null) {
            config.withDockerTlsVerify(false);
        } else {
            config.withDockerCertPath(DockerEngineCertificate);
        }
        return config.build();
    }

    static private DockerClient getClient(String dockerEngineUrl, String dockerEngineCertificate) {
        return DockerClientBuilder.getInstance(getConfig(dockerEngineUrl, dockerEngineCertificate)).build();
    }

    static String executeCommand(String dockerEngineUrl, String dockerEngineCertificate, String containerId, String script) throws InterruptedException {
        LOG.info("Executing shell command: {}", script);

        try (OutputStream output = getStringOutputStream()) {
            DockerClient dockerClient = getClient(dockerEngineUrl, dockerEngineCertificate);

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
            LOG.info("Execution log:\n {}", log);
            return log;
        } catch (IOException e) {
            LOG.error("Could not execute command", e);
        }

        return "";
    }

    private static OutputStream getStringOutputStream() {
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

    static void waitForAvailability(String dockerEngineURL, String dockerEngineCertificate, String containerId) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 25000;
        while (System.currentTimeMillis() < endTime) {
            try {
                executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "pwd");
                // if we can execute pwd without issues ssh is up!
                break;
            } catch (Exception e) {
                LOG.error("Could not wait for availability", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    LOG.error("Could not wait for availability", e1);
                }
            }
        }
    }

    static String replaceHome(String dockerEngineURL, String dockerEngineCertificate, String host, String command, boolean all) {
        if (command.contains("~")) {
            try {
                String pwd = executeCommand(dockerEngineURL, dockerEngineCertificate, host, "pwd").trim();
                LOG.info("Replaced ~ with user home ('{}'): '{}'", pwd, command);
                return all ? command.replaceAll("~", pwd) : command.replaceFirst("~", pwd);
            } catch (InterruptedException e) {
                LOG.error("Could not replace home", e);
            }
        }

        return command;
    }

    static void uploadFile(String dockerEngineURL, String dockerEngineCertificate, String containerId, String uploadFile, String target) throws InterruptedException, UnsupportedEncodingException {
        LOG.info("Uploading file '{}' to '{}'...", uploadFile, target);

        int end = target.lastIndexOf('/');
        String folders = target.substring(0, end);
        executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "mkdir -p " + folders);

        DockerClient dockerClient = getClient(dockerEngineURL, dockerEngineCertificate);
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath(URLEncoder.encode(folders, "UTF-8"))
                .withHostResource(uploadFile)
                .exec();

        LOG.info("Successfully uploaded file!");
    }

    static void dos2unix(String dockerEngineURL, String dockerEngineCertificate, String containerId, String absolutePath) throws InterruptedException {
        // Check if file should be converted
        if (!absolutePath.endsWith(".sh")) {
            System.out.println("Skipping converting file to unix since file " + absolutePath + " does not end with .sh");
            return;
        }

        // Ensure dos2unix package is installed
        ensurePackage(dockerEngineURL, dockerEngineCertificate, containerId, "dos2unix");

        // Convert file to Unix
        LOG.info("Convert file " + absolutePath + " to Unix");
        executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "dos2unix " + absolutePath + " " + absolutePath);

        // Success
        LOG.info("Successfully converted file " + absolutePath + " to Unix");
    }

    static void ensurePackage(String dockerEngineURL, String dockerEngineCertificate, String containerId, String packageName) throws InterruptedException {
        String check = executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "apt -qq list" + packageName);
        if (check.contains("[installed]")) {
            LOG.info("Installing package {}", packageName);
            executeCommand(dockerEngineURL, dockerEngineCertificate, containerId, "apt update -y && apt install -yq " + packageName);
            LOG.info("Installed package {}", packageName);
        } else {
            LOG.info("Package {} is already installed", packageName);
        }
    }
}
