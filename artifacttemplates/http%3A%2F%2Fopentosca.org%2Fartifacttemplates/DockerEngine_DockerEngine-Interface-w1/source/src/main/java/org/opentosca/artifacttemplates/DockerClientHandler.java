package org.opentosca.artifacttemplates;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerClientHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DockerClientHandler.class);

    protected static DefaultDockerClientConfig getConfig(String DockerEngineURL, String DockerEngineCertificate) {
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

    /**
     * Check if the image is already available at the Docker host
     *
     * @param image the image to check availability for
     * @return The image ID if the image is available, null otherwise
     */
    protected static String isImageAvailable(final String image, String dockerEngineURL, String dockerEngineCertificate) {
        LOG.info("Searching available Images...");
        DockerClient client = DockerClientBuilder.getInstance(getConfig(dockerEngineURL, dockerEngineCertificate)).build();
        for (final Image availImage : client.listImagesCmd().exec()) {
            for (final String tag : availImage.getRepoTags()) {
                if (tag.startsWith(image)) {
                    return availImage.getId();
                }
            }
        }

        return null;
    }

    /**
     * Returns the port to which a docker container is bound.
     *
     * @param dockerClient The docker client where the container is running.
     * @param containerID  The ID of the container
     * @param searchedPort The inner port of the container
     * @return The outer port to which the specified inner port of the container is bound.
     */
    protected static int findPort(final DockerClient dockerClient, final String containerID, final int searchedPort) {
        for (final Container container : dockerClient.listContainersCmd().exec()) {
            if (container.getId().equals(containerID)) {
                for (final ContainerPort port : container.getPorts()) {
                    if (port != null && port.getPrivatePort() != null && port.getPrivatePort() == searchedPort) {
                        return port.getPublicPort();
                    }
                }
            }
        }
        return -1;
    }
}
