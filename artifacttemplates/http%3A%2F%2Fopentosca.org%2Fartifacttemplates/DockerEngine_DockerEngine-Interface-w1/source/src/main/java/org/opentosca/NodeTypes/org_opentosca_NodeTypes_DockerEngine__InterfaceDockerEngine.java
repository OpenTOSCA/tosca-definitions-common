package org.opentosca.NodeTypes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

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
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.io.Files;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService(targetNamespace = "http://NodeTypes.opentosca.org/")
public class org_opentosca_NodeTypes_DockerEngine__InterfaceDockerEngine extends AbstractIAService {

    private static final Logger LOG = LoggerFactory.getLogger(org_opentosca_NodeTypes_DockerEngine__InterfaceDockerEngine.class);

    @WebMethod
    @SOAPBinding
    @Oneway
    public void startContainer(
            @WebParam(name = "DockerEngineURL", targetNamespace = "http://NodeTypes.opentosca.org/") final String DockerEngineURL,
            @WebParam(name = "DockerEngineCertificate", targetNamespace = "http://NodeTypes.opentosca.org/") final String DockerEngineCertificate,
            @WebParam(name = "ContainerImage", targetNamespace = "http://NodeTypes.opentosca.org/") final String ContainerImage,
            @WebParam(name = "ContainerPorts", targetNamespace = "http://NodeTypes.opentosca.org/") final String ContainerPorts,
            @WebParam(name = "ContainerEnv", targetNamespace = "http://NodeTypes.opentosca.org/") final String ContainerEnv,
            @WebParam(name = "ImageLocation", targetNamespace = "http://NodeTypes.opentosca.org/") final String ImageLocation,
            @WebParam(name = "PrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") final String PrivateKey,
            @WebParam(name = "Links", targetNamespace = "http://NodeTypes.opentosca.org/") final String Links,
            @WebParam(name = "Devices", targetNamespace = "http://NodeTypes.opentosca.org/") final String Devices,
            @WebParam(name = "RemoteVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") final String RemoteVolumeData,
            @WebParam(name = "HostVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") final String HostVolumeData,
            @WebParam(name = "ContainerMountPath", targetNamespace = "http://NodeTypes.opentosca.org/") final String ContainerMountPath,
            @WebParam(name = "VMIP", targetNamespace = "http://NodeTypes.opentosca.org/") final String VMIP,
            @WebParam(name = "VMPrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") final String VMPrivateKey) {
        // create connection to the docker engine

        DefaultDockerClientConfig config = getConfig(DockerEngineURL, DockerEngineCertificate);

        try (DockerClient dockerClient = DockerClientBuilder
                .getInstance(config)
                .build()) {

            // cut ip address out of DockerEngineURL
            final String ipAddress = DockerEngineURL.split(":")[1].substring(2);

            LOG.info("Try to connect to " + ipAddress);

            // create image or pull it if a remote image shall be used
            String image = null;
            if (ContainerImage == null) { // either ContainerImage or ImageLocation
                // has to be set
                image = "da/" + System.currentTimeMillis();

                File basePath = new File(ImageLocation);

                try {
                    final URI dockerImageURI = new URI(ImageLocation);

                    final String[] pathSplit = dockerImageURI.getRawPath().split("/");
                    final String fileName = pathSplit[pathSplit.length - 1];

                    if (dockerImageURI.isAbsolute() | new File(dockerImageURI.toString()).exists()) {
                        final File tempDir = Files.createTempDir();
                        final File tempUnpackDir = Files.createTempDir();
                        File tempFile = new File(tempDir, fileName);

                        if (dockerImageURI.toString().startsWith("http")) {

                            downloadFile(dockerImageURI, tempFile);
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
                    if (isImageAvailable(image, DockerEngineURL, DockerEngineCertificate) == null) {
                        LOG.info("Image {} not found", image);
                        LOG.info("Starting to load image from tar.gz file at {}", basePath);

                        // Create new Docker client
                        // Reason: The loadImageCmd() does not close the connection to the Docker host.
                        // If no further connections are available the client blocks forever.
                        // cf. https://github.com/docker-java/docker-java/issues/841
                        try (FileInputStream fis = new FileInputStream(basePath)) {
                            DockerClientBuilder
                                    .getInstance(getConfig(DockerEngineURL, DockerEngineCertificate))
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
                image = isImageAvailable(ContainerImage, DockerEngineURL, DockerEngineCertificate);
                if (image == null) {
                    LOG.info("App container image not yet available. Pulling image...");
                    dockerClient.pullImageCmd(ContainerImage)
                            .exec(new PullImageResultCallback())
                            .awaitCompletion();
                    image = ContainerImage;
                }
            }

            // expose ports if needed for the container
            final List<ExposedPort> exposedPorts = new ArrayList<>();
            Ports portBindings = new Ports();
            if (ContainerPorts != null) {
                for (final String portMapping : ContainerPorts.split(";")) {
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
            if (ContainerEnv != null) {
                environmentVariables = Arrays.asList(ContainerEnv.split(";"));
            }

            LOG.info("Will start container with following environment variables:\n\t{}", environmentVariables);

            final List<Link> links = new ArrayList<>();
            if (Links != null) {
                final String[] idsToLinkSplit = Links.split(";");
                for (final String idToLink : idsToLinkSplit) {
                    LOG.info("Will link container to container with id {}", idToLink);
                    links.add(new Link(idToLink.trim(), null));
                }
            }

            final List<Device> devices = new ArrayList<>();
            if (Devices != null) {
                final String[] devMappingSplit = Devices.split(";");
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

            if (ContainerMountPath != null && !ContainerMountPath.isEmpty()) {
                volume = new Volume(ContainerMountPath);

                final CreateContainerResponse volumeContainer = dockerClient.createContainerCmd("phusion/baseimage:latest")
                        .withBinds(new Bind(hostVolPath, volume, AccessMode.rw))
                        .withVolumes(volume).exec();
                LOG.info("Created volume container {}", volumeContainer.getId());
                dockerClient.startContainerCmd(volumeContainer.getId()).exec();
                LOG.info("Started volume container {}", volumeContainer.getId());
                try {
                    final ExecCreateCmdResponse execCmdResp = dockerClient
                            .execCreateCmd(volumeContainer.getId())
                            .withCmd("mkdir", "-p", ContainerMountPath)
                            .exec();
                    dockerClient.execStartCmd(execCmdResp.getId())
                            .start()
                            .awaitCompletion();

                    if (RemoteVolumeData != null) {
                        // volumeData is a set of http paths pointing to tar files
                        final String[] dataPaths = RemoteVolumeData.split(";");

                        for (final String dataPath : dataPaths) {
                            final File volumeFile = downloadFile(dataPath);

                            if (volumeFile != null) {
                                final File volumeTarFile = createTempTarFromFile(volumeFile);

                                dockerClient.copyArchiveToContainerCmd(volumeContainer.getId())
                                        .withRemotePath(ContainerMountPath)
                                        .withTarInputStream(new FileInputStream(volumeTarFile)).exec();

                                final ExecCreateCmdResponse execChmodCmdResp = dockerClient
                                        .execCreateCmd(volumeContainer.getId())
                                        .withCmd("chmod", "600", ContainerMountPath + "/" + volumeFile.getName()).exec();
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

            CreateContainerResponse container = null;
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
                        .append(findPort(dockerClient, container.getId(), port.getPort()));
                first = false;
            }

            // this HashMap holds the return parameters of this operation.
            final HashMap<String, String> returnParameters = new HashMap<>();

            returnParameters.put("ContainerPorts", portMapping.toString());
            returnParameters.put("ContainerID", container.getId());
            returnParameters.put("ContainerIP", ipAddress);
            returnParameters.put("ContainerName", containerName);

            sendResponse(returnParameters);
        } catch (final Exception e) {
            LOG.error("Error while closing docker client.", e);
        }
    }

    private DefaultDockerClientConfig getConfig(String DockerEngineURL, String DockerEngineCertificate) {
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

    private File downloadFile(final String url) {
        try {
            final URI dockerImageURI = new URI(url);

            final String[] pathSplit = dockerImageURI.getRawPath().split("/");
            final String fileName = pathSplit[pathSplit.length - 1];

            final File tempDir = Files.createTempDir();
            final File tempFile = new File(tempDir, fileName);

            downloadFile(dockerImageURI, tempFile);
            return tempFile;
        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void downloadFile(URI dockerImageURI, File tempFile) throws IOException {
        final URLConnection connection = dockerImageURI.toURL().openConnection();
        connection.setRequestProperty("Accept", "application/octet-stream");

        try (final InputStream input = connection.getInputStream()) {
            final byte[] buffer = new byte[4096];
            int n;

            try (final OutputStream output = new FileOutputStream(tempFile)) {
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
        }
    }

    private File createTempTarFromFile(final File file) {
        final TarArchiveEntry entry = new TarArchiveEntry(file, file.getName());

        File tarArchive = null;
        try {
            tarArchive = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".tar");
            final TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(tarArchive));
            out.putArchiveEntry(entry);
            IOUtils.copy(new FileInputStream(file), out);
            out.closeArchiveEntry();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return tarArchive;
    }

    /**
     * Check if the image is already available at the Docker host
     *
     * @param image the image to check availability for
     * @return The image ID if the image is available, null otherwise
     */
    private String isImageAvailable(final String image, String dockerEngineURL, String dockerEngineCertificate) {
        LOG.info("Searching available Images...");
        DockerClient client = DockerClientBuilder
                .getInstance(getConfig(dockerEngineURL, dockerEngineCertificate))
                .build();
        for (final Image availImage : client.listImagesCmd().exec()) {
            for (final String tag : availImage.getRepoTags()) {
                if (tag.startsWith(image)) {
                    return availImage.getId();
                }
            }
        }

        return null;
    }

    @WebMethod
    @SOAPBinding
    @Oneway
    public void removeContainer(
            @WebParam(name = "DockerEngineURL", targetNamespace = "http://NodeTypes.opentosca.org/") final String DockerEngineURL,
            @WebParam(name = "DockerEngineCertificate", targetNamespace = "http://NodeTypes.opentosca.org/") final String DockerEngineCertificate,
            @WebParam(name = "ContainerID", targetNamespace = "http://NodeTypes.opentosca.org/") final String ContainerID) {
        // this HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        try (final DockerClient dockerClient = DockerClientBuilder
                .getInstance(getConfig(DockerEngineURL, DockerEngineCertificate))
                .build()) {
            // stop ssh and real container together
            for (final String id : ContainerID.split(";")) {
                // stop and remove container
                LOG.info("Stopping container {}...", id);
                dockerClient.stopContainerCmd(id).exec();
                LOG.info("Removing container {}...", id);
                dockerClient.removeContainerCmd(id).exec();
                LOG.info("Stopped and removed container {}", id);
            }

            returnParameters.put("Result", "Stopped and Removed container " + ContainerID);

            sendResponse(returnParameters);
        } catch (final IOException e) {
            LOG.error("Error closing the Docker client", e);
        }
    }

    /**
     * Returns the port to which a docker container is bound.
     *
     * @param dockerClient The docker client where the container is running.
     * @param containerID  The ID of the container
     * @param searchedPort The inner port of the container
     * @return The outer port to which the specified inner port of the container is bound.
     */
    private int findPort(final DockerClient dockerClient, final String containerID, final int searchedPort) {
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
