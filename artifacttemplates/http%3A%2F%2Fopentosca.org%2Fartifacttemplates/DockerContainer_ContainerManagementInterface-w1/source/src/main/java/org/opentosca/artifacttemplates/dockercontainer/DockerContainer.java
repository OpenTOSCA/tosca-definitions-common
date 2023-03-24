package org.opentosca.artifacttemplates.dockercontainer;

import java.io.IOException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
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

    public void awaitAvailability() throws InterruptedException {
        LOG.info("Checking if container is available");
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 25000;
        InterruptedException error = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                // if command can be executed without issues ssh is up
                execCommand("echo container availability check");
                LOG.info("Container is available");
                return;
            } catch (InterruptedException e) {
                LOG.error("Could not check if container is available", e);
                error = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    LOG.error("Could not check if container is available", e1);
                }
            }
        }
        if (error != null) {
            throw error;
        }
    }

    public String execCommand(String command) throws InterruptedException {
        LOG.info("Executing command on container: {}", command);

        ExecCreateCmdResponse execCreateCmdResponse = client.execCreateCmd(containerId)
                .withTty(true)
                .withAttachStdout(true)
                .withCmd("/bin/bash", "-c", command)
                .exec();

        try (AttachContainerCallback callback = client.execStartCmd(execCreateCmdResponse.getId())
                .withTty(true)
                .withDetach(false)
                .exec(new AttachContainerCallback())) {
            callback.awaitCompletion();
            return callback.builder.toString();
        } catch (IOException e) {
            LOG.error("Error while executing...", e);
        }

        return "";
    }

    /**
     * This function replaces any "~" in the command with the output of pwd.
     * <p>
     * For example, <code>sleep 1 && mkdir -p ~/some/path/~/dir && rmdir ~/some/other/path</code> will be transformed
     * to
     * <code>sleep 1 && mkdir -p /some/path/dir && rmdir /some/other/path" if pwd is "/"</code>.
     * <p>
     * Note, the example also shows that the second "~" of the mkdir command is also replaced. This might lead to
     * unexpected behaviour.
     * <p>
     * Note, pwd does not necessarily return the home directory.
     */
    public String replaceHome(String command) throws InterruptedException {
        if (command.contains("~/")) {
            String pwd = execCommand("pwd").trim();

            String replaced;
            if (pwd.endsWith("/")) {
                replaced = command.replaceAll("~/", pwd);
            } else {
                replaced = command.replaceAll("~", pwd);
            }

            LOG.info("Replaced '~' in '{}' with '{}' which results in '{}'", command, pwd, replaced);
            return replaced;
        }
        return command;
    }

    public void uploadFile(String source, String target) throws InterruptedException, NotFoundException {
        LOG.info("Uploading host file '{}' to container at '{} '", source, target);

        String directory = target.substring(0, target.lastIndexOf('/'));
        LOG.info("Creating directory on the container: '{}'", directory);
        execCommand("mkdir -p " + directory);

        LOG.info("Copy file to container");
        client.copyArchiveToContainerCmd(containerId)
                .withRemotePath(directory)
                .withHostResource(source)
                .exec();

        LOG.info("Successfully uploaded file to container");
    }

    public void convertToUnix(String target) throws InterruptedException {
        LOG.info("Converting file '{}' to unix on container", target);

        if (!target.endsWith(".sh")) {
            LOG.info("Skipping converting file to unix since file '{}' does not end with .sh", target);
            return;
        }

        ensurePackage("dos2unix");
        execCommand("dos2unix " + target + " " + target);

        LOG.info("Successfully converted file '{}' to unix on container", target);
    }

    public void ensurePackage(String name) throws InterruptedException {
        String check = execCommand("apt -qq list " + name);
        if (!check.contains("[installed]")) {
            LOG.info("Installing package '{}' on container", name);
            execCommand("apt update -y && apt install -yq " + name);
            LOG.info("Installed package '{}' on container", name);
        } else {
            LOG.info("Package '{}' is already installed on container", name);
        }
    }

    private DockerClient getClient(String dockerEngineURL, String dockerEngineCertificate) {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerEngineURL)
                .withDockerTlsVerify(false);

        if (dockerEngineCertificate != null && !dockerEngineCertificate.isBlank()) {
            // TODO Save the file first to temp
            configBuilder.withDockerCertPath(dockerEngineCertificate);
        }

        DefaultDockerClientConfig config = configBuilder.build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    public static class AttachContainerCallback extends ResultCallback.Adapter<Frame> {

        StringBuilder builder = new StringBuilder();

        @Override
        public void onNext(Frame frame) {
            String result = new String(frame.getPayload());
            System.out.println(result);

            builder.append(result);

            super.onNext(frame);
        }
    }
}
