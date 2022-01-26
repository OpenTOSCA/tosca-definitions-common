package org.opentosca.artifacttemplates;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBElement;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opentosca.nodetypes.InvokeResponse;
import org.opentosca.nodetypes.RemoveContainerRequest;
import org.opentosca.nodetypes.StartContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.io.Files;

import net.lingala.zip4j.ZipFile;

@Endpoint
public class DockerEngineInterfaceDockerEngineEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(DockerEngineInterfaceDockerEngineEndpoint.class);

    @PayloadRoot(namespace = Constants.NAMESPACE_URI, localPart = "startContainerRequest")
    public void startContainer(@RequestPayload JAXBElement<StartContainerRequest> requestJaxb, MessageContext messageContext) {
        LOG.info("Received startContainer request!");
        StartContainerRequest request = requestJaxb.getValue();

        // retrieve the SOAP headers, e.g., to get the message ID
        Node messageIdNode = SoapUtil.getHeaderFieldByName(messageContext, Constants.MESSAGE_ID_HEADER);
        Node replyToNode = SoapUtil.getHeaderFieldByName(messageContext, Constants.REPLY_TO_HEADER);
        if (Objects.isNull(messageIdNode) || Objects.isNull(replyToNode)) {
            LOG.error("Unable to retrieve message ID and reply to headers from received SOAP request!");
            return;
        }
        String messageId = messageIdNode.getTextContent();
        String replyTo = replyToNode.getFirstChild().getTextContent();
        LOG.info("Retrieved message ID: {}", messageId);
        LOG.info("ReplyTo address: {}", replyTo);

        // create connection to the docker engine
        if (Objects.isNull(request.getDockerEngineURL())) {
            LOG.error("Docker Engine URL not defined in SOAP request!");
            InvokeResponse invokeResponse = new InvokeResponse();
            invokeResponse.setMessageID(messageId);
            invokeResponse.setError("Docker Engine URL must be defined to start a container!");

            SoapUtil.sendSoapResponse(invokeResponse, replyTo);
            return;
        }
        DefaultDockerClientConfig config = DockerClientHandler.getConfig(request.getDockerEngineURL(), request.getDockerEngineCertificate());

        try (DockerClient dockerClient = DockerClientBuilder.getInstance(config).build()) {

            // cut ip address out of DockerEngineURL
            final String ipAddress = request.getDockerEngineURL().split(":")[1].substring(2);

            LOG.info("Try to connect to " + ipAddress);

            // create image or pull it if a remote image shall be used
            String image = null;
            if (request.getContainerImage() == null) { // either ContainerImage or ImageLocation
                // has to be set
                image = "da/" + System.currentTimeMillis();

                File basePath = new File(request.getImageLocation());

                try {
                    final URI dockerImageURI = new URI(request.getImageLocation());

                    final String[] pathSplit = dockerImageURI.getRawPath().split("/");
                    final String fileName = pathSplit[pathSplit.length - 1];

                    if (dockerImageURI.isAbsolute() | new File(dockerImageURI.toString()).exists()) {
                        final File tempDir = Files.createTempDir();
                        final File tempUnpackDir = Files.createTempDir();
                        File tempFile = new File(tempDir, fileName);

                        if (dockerImageURI.toString().startsWith("http")) {

                            FileHandler.downloadFile(dockerImageURI, tempFile);
                        } else {
                            tempFile = basePath;
                        }

                        if (fileName.endsWith("zip")) {
                            final ZipFile zipFile = new ZipFile(tempFile);
                            zipFile.extractAll(tempUnpackDir.toString());

                            basePath = new File(tempUnpackDir, "Dockerfile");
                            LOG.info("Unpacked DockerContainer Files, base Dockerfile at {}",
                                    basePath.getAbsolutePath());
                        } else if (fileName.endsWith("tar.gz")) {
                            basePath = tempFile;

                            // open tarball and look into repository file for the image tag
                            try (final TarArchiveInputStream tarIn = new TarArchiveInputStream(
                                    new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))))) {

                                TarArchiveEntry entry;
                                while ((entry = tarIn.getNextTarEntry()) != null) {
                                    if (entry.getName().trim().equals("repositories")) {
                                        final File entryFile = new File(tempDir, entry.getName());

                                        try (final OutputStream out = new FileOutputStream(entryFile)) {
                                            IOUtils.copy(tarIn, out);
                                        }

                                        final ObjectMapper objMapper = new ObjectMapper();
                                        final String repositoryContents = FileUtils.readFileToString(entryFile);
                                        final JsonNode rootNode = objMapper.readTree(repositoryContents);

                                        if (rootNode.size() == 1) {
                                            final Iterator<String> fieldNames = rootNode.fieldNames();
                                            image = fieldNames.next();

                                            // get tag
                                            final JsonNode tagNode = rootNode.get(image);

                                            if (tagNode.size() == 1) {
                                                image += ":" + tagNode.fieldNames().next();
                                            }
                                        }

                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (final URISyntaxException | IOException e) {
                    e.printStackTrace();
                }

                if (basePath.getName().contains("Dockerfile")) {
                    try (final BuildImageResultCallback callback = new BuildImageResultCallback() {
                        @Override
                        public void onNext(final BuildResponseItem item) {
                            LOG.info("Found response item: {}", item);
                            super.onNext(item);
                        }
                    }) {
                        LOG.info("Starting to build image from zip file at {}", basePath);
                        dockerClient.buildImageCmd(basePath)
                                .withTags(Collections.singleton(image))
                                .exec(callback)
                                .awaitImageId();
                    } catch (final IOException e) {
                        System.err.println("Error while building image!");
                        e.printStackTrace();
                    }
                } else {
                    // the tar.gz case
                    if (DockerClientHandler.isImageAvailable(image, request.getDockerEngineURL(), request.getDockerEngineCertificate()) == null) {
                        LOG.info("Image {} not found", image);
                        LOG.info("Starting to load image from tar.gz file at {}", basePath);

                        // Create new Docker client
                        // Reason: The loadImageCmd() does not close the connection to the Docker host.
                        // If no further connections are available the client blocks forever.
                        // cf. https://github.com/docker-java/docker-java/issues/841
                        try (FileInputStream fis = new FileInputStream(basePath)) {
                            DockerClientBuilder
                                    .getInstance(DockerClientHandler.getConfig(request.getDockerEngineURL(), request.getDockerEngineCertificate()))
                                    .build()
                                    .loadImageCmd(fis).exec();
                        } catch (final FileNotFoundException e) {
                            System.err.println("Could not find image file!");
                            e.printStackTrace();
                        }
                    } else {
                        LOG.info("Image {} already available skipping upload", image);
                    }
                }
            } else {
                image = DockerClientHandler.isImageAvailable(request.getContainerImage(), request.getDockerEngineURL(),
                        request.getDockerEngineCertificate());
                if (image == null) {
                    LOG.info("App container image not yet available. Pulling image...");
                    dockerClient.pullImageCmd(request.getContainerImage())
                            .exec(new PullImageResultCallback())
                            .awaitCompletion();
                    image = request.getContainerImage();
                }
            }

            // expose ports if needed for the container
            final List<ExposedPort> exposedPorts = new ArrayList<>();
            Ports portBindings = new Ports();
            if (request.getContainerPorts() != null) {
                for (final String portMapping : request.getContainerPorts().split(";")) {
                    if (portMapping.trim().isEmpty()) {
                        continue;
                    }

                    final String[] portMapKV = portMapping.split(",");
                    if (portMapKV.length > 0 && Arrays.stream(portMapKV).noneMatch(String::isEmpty)) {
                        final ExposedPort tempPort = ExposedPort.tcp(Integer.parseInt(portMapKV[0]));
                        Integer externalPort = null;

                        boolean randomPort = false;
                        if (portMapKV.length > 1 && portMapKV[1] != null && !portMapKV[1].isEmpty()) {
                            externalPort = Integer.parseInt(portMapKV[1]);
                        } else {
                            randomPort = true;
                        }
                        exposedPorts.add(tempPort);

                        if (!randomPort) {
                            LOG.info("Creating PortBinding {}:{}", tempPort, externalPort);
                            portBindings.bind(tempPort, Ports.Binding.bindPort(externalPort));
                        } else {
                            // map to random port
                            portBindings.bind(tempPort, Ports.Binding.empty());
                        }
                    }
                }
            }

            // parse environment variables
            List<String> environmentVariables = new ArrayList<>();
            if (request.getContainerEnv() != null) {
                environmentVariables = Arrays.asList(request.getContainerEnv().split(";"));
            }

            LOG.info("Will start container with following environment variables:\n\t{}", environmentVariables);

            final List<Link> links = new ArrayList<>();
            if (request.getLinks() != null) {
                final String[] idsToLinkSplit = request.getLinks().split(";");
                for (final String idToLink : idsToLinkSplit) {
                    LOG.info("Will link container to container with id {}", idToLink);
                    links.add(new Link(idToLink.trim(), null));
                }
            }

            final List<Device> devices = new ArrayList<>();
            if (request.getDevices() != null) {
                final String[] devMappingSplit = request.getDevices().split(";");
                for (final String devMapping : devMappingSplit) {
                    final String[] devMapSplit = devMapping.split("=");
                    if (devMapSplit.length == 2) {
                        LOG.info("Will add device {}:{}", devMapSplit[0], devMapSplit[1]);
                        devices.add(new Device("mrw", devMapSplit[0], devMapSplit[1]));
                    }
                }
            }

            Volume volume = null;
            final String hostVolPath = "/volumeFor" + image.replace("/", "_").replace(":", "") + System.currentTimeMillis();

            if (request.getContainerMountPath() != null && !request.getContainerMountPath().isEmpty()) {
                volume = new Volume(request.getContainerMountPath());

                final CreateContainerResponse volumeContainer = dockerClient.createContainerCmd("phusion/baseimage:latest")
                        .withBinds(new Bind(hostVolPath, volume, AccessMode.rw))
                        .withVolumes(volume).exec();
                LOG.info("Created volume container {}", volumeContainer.getId());
                dockerClient.startContainerCmd(volumeContainer.getId()).exec();
                LOG.info("Started volume container {}", volumeContainer.getId());
                try {
                    final ExecCreateCmdResponse execCmdResp = dockerClient
                            .execCreateCmd(volumeContainer.getId())
                            .withCmd("mkdir", "-p", request.getContainerMountPath())
                            .exec();
                    dockerClient.execStartCmd(execCmdResp.getId())
                            .start()
                            .awaitCompletion();

                    if (request.getRemoteVolumeData() != null) {
                        // volumeData is a set of http paths pointing to tar files
                        final String[] dataPaths = request.getRemoteVolumeData().split(";");

                        for (final String dataPath : dataPaths) {
                            final File volumeFile = FileHandler.downloadFile(dataPath);

                            if (volumeFile != null) {
                                final File volumeTarFile = FileHandler.createTempTarFromFile(volumeFile);

                                dockerClient.copyArchiveToContainerCmd(volumeContainer.getId())
                                        .withRemotePath(request.getContainerMountPath())
                                        .withTarInputStream(new FileInputStream(volumeTarFile)).exec();

                                final ExecCreateCmdResponse execChmodCmdResp = dockerClient
                                        .execCreateCmd(volumeContainer.getId())
                                        .withCmd("chmod", "600", request.getContainerMountPath() + "/" + volumeFile.getName()).exec();
                                dockerClient.execStartCmd(execChmodCmdResp.getId())
                                        .start()
                                        .awaitCompletion();
                            }
                        }
                    }
                } catch (final IOException | InterruptedException e) {
                    System.err.println("Error while mounting Volume!");
                    e.printStackTrace();
                }
            }

            CreateContainerResponse container;
            if (volume != null) {
                container = dockerClient.createContainerCmd(image)
                        .withEnv(environmentVariables)
                        .withTty(true)
                        .withLinks(links)
                        .withExposedPorts(exposedPorts)
                        .withPortBindings(portBindings)
                        .withBinds(new Bind(hostVolPath, volume))
                        .withVolumes(volume)
                        .withDevices(devices)
                        .withCmd("-v")
                        .exec();
            } else {
                // start container
                container = dockerClient.createContainerCmd(image)
                        .withExposedPorts(exposedPorts)
                        .withPortBindings(portBindings)
                        .withEnv(environmentVariables)
                        .withTty(true)
                        .withLinks(links)
                        .withDevices(devices)
                        .exec();
            }

            LOG.info("Created container {}", container.getId());
            dockerClient
                    .startContainerCmd(container.getId())
                    .exec();

            // get name of the new container
            final String containerName = dockerClient.inspectContainerCmd(container.getId())
                    .exec()
                    .getName().substring(1);
            LOG.info("Started container {} with name {}", container.getId(), containerName);

            // return outer ports for the requested inner ports
            StringBuilder portMapping = new StringBuilder();
            boolean first = true;
            for (final ExposedPort port : exposedPorts) {
                if (!first) {
                    portMapping.append(",");
                }
                portMapping.append(port.getPort())
                        .append("-->")
                        .append(DockerClientHandler.findPort(dockerClient, container.getId(), port.getPort()));
                first = false;
            }

            // create response and send back
            InvokeResponse invokeResponse = new InvokeResponse();
            invokeResponse.setMessageID(messageId);
            invokeResponse.setContainerPorts(portMapping.toString());
            invokeResponse.setContainerID(container.getId());
            invokeResponse.setContainerIP(ipAddress);
            invokeResponse.setContainerName(containerName);

            SoapUtil.sendSoapResponse(invokeResponse, replyTo);
        } catch (final Exception e) {
            LOG.error("Error while closing docker client.", e);
            InvokeResponse invokeResponse = new InvokeResponse();
            invokeResponse.setMessageID(messageId);
            invokeResponse.setError(e.getMessage());

            SoapUtil.sendSoapResponse(invokeResponse, replyTo);
        }
    }

    @PayloadRoot(namespace = Constants.NAMESPACE_URI, localPart = "removeContainerRequest")
    public void removeContainer(@RequestPayload JAXBElement<RemoveContainerRequest> requestJaxb, MessageContext messageContext) {
        LOG.info("Received removeContainer request!");
        RemoveContainerRequest request = requestJaxb.getValue();

        // retrieve the SOAP headers, e.g., to get the message ID
        Node messageIdNode = SoapUtil.getHeaderFieldByName(messageContext, Constants.MESSAGE_ID_HEADER);
        Node replyToNode = SoapUtil.getHeaderFieldByName(messageContext, Constants.REPLY_TO_HEADER);
        if (Objects.isNull(messageIdNode) || Objects.isNull(replyToNode)) {
            LOG.error("Unable to retrieve message ID and reply to headers from received SOAP request!");
            return;
        }
        String messageId = messageIdNode.getTextContent();
        String replyTo = replyToNode.getFirstChild().getTextContent();
        LOG.info("Retrieved message ID: {}", messageId);
        LOG.info("ReplyTo address: {}", replyTo);

        try (final DockerClient dockerClient = DockerClientBuilder
                .getInstance(DockerClientHandler.getConfig(request.getDockerEngineURL(), request.getDockerEngineCertificate()))
                .build()) {
            // stop ssh and real container together
            for (final String id : request.getContainerID().split(";")) {
                // stop and remove container
                LOG.info("Stopping container {}...", id);
                dockerClient.stopContainerCmd(id).exec();
                LOG.info("Removing container {}...", id);
                dockerClient.removeContainerCmd(id).exec();
                LOG.info("Stopped and removed container {}", id);
            }

            // create response and send back
            InvokeResponse invokeResponse = new InvokeResponse();
            invokeResponse.setMessageID(messageId);
            invokeResponse.setResult("Stopped and Removed container " + request.getContainerID());

            SoapUtil.sendSoapResponse(invokeResponse, replyTo);
        } catch (final IOException e) {
            LOG.error("Error closing the Docker client", e);
            InvokeResponse invokeResponse = new InvokeResponse();
            invokeResponse.setMessageID(messageId);
            invokeResponse.setError(e.getMessage());

            SoapUtil.sendSoapResponse(invokeResponse, replyTo);
        }
    }
}
