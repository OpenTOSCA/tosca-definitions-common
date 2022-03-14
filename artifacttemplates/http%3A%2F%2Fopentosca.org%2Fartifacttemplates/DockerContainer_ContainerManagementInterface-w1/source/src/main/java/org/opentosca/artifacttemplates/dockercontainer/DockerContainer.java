package org.opentosca.artifacttemplates.dockercontainer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerContainer {

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainer.class);

    private final DockerClient client;
    private final String containerId;

    public DockerContainer(String dockerEngineURL, String dockerEngineCertificate, String containerId) {
        this.client = getClient(dockerEngineURL, dockerEngineCertificate);
        this.containerId = containerId;
    }

    public void awaitAvailability() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 25000;
        while (System.currentTimeMillis() < endTime) {
            try {
                execCommand("pwd");
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

    public String execCommand(String command) throws InterruptedException {
        LOG.info("Executing shell command: {}", command);

        try (OutputStream output = getStringOutputStream()) {
            ExecCreateCmdResponse execCreateCmdResponse = client.execCreateCmd(containerId)
                    .withTty(true)
                    .withAttachStdout(true)
                    .withCmd("/bin/bash", "-c", command)
                    .exec();

            client.execStartCmd(execCreateCmdResponse.getId())
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

    public String replaceHome(String command) throws InterruptedException {
        if (command.startsWith("~")) {
            String pwd = execCommand("pwd").trim();
            LOG.info("Replaced ~ with user home ('{}'): '{}'", pwd, command);
            return command.replaceFirst("~", pwd);
        }
        return command;
    }

    public void uploadFile(String source, String target) throws InterruptedException {
        LOG.info("Uploading file '{}' to '{}'...", source, target);

        int end = target.lastIndexOf('/');
        String folders = target.substring(0, end);
        execCommand("mkdir -p " + folders);

        client.copyArchiveToContainerCmd(containerId)
                .withRemotePath(URLEncoder.encode(folders, StandardCharsets.UTF_8))
                .withHostResource(source)
                .exec();

        LOG.info("Successfully uploaded file!");
    }

    public void convertToUnix(String target) throws InterruptedException {
        LOG.info("Converting file '{}' to Unix", target);

        if (!target.endsWith(".sh")) {
            LOG.info("Skipping converting file to unix since file '{}' does not end with .sh", target);
            return;
        }

        ensurePackage("dos2unix");
        execCommand("dos2unix " + target + " " + target);

        LOG.info("Successfully converted file " + target + " to Unix");
    }

    public void ensurePackage(String name) throws InterruptedException {
        String check = execCommand("apt -qq list" + name);
        if (check.contains("[installed]")) {
            LOG.info("Installing package {}", name);
            execCommand("apt update -y && apt install -yq " + name);
            LOG.info("Installed package {}", name);
        } else {
            LOG.info("Package {} is already installed", name);
        }
    }

    private DockerClient getClient(String dockerEngineURL, String dockerEngineCertificate) {
        return DockerClientBuilder
                .getInstance(getConfig(dockerEngineURL, dockerEngineCertificate))
                .build();
    }

    private DefaultDockerClientConfig getConfig(String dockerEngineURL, String dockerEngineCertificate) {
        DefaultDockerClientConfig.Builder config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(dockerEngineURL)
                .withApiVersion("1.21");

        if (dockerEngineCertificate == null) {
            config.withDockerTlsVerify(false);
        } else {
            config.withDockerCertPath(dockerEngineCertificate);
        }
        return config.build();
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
}
