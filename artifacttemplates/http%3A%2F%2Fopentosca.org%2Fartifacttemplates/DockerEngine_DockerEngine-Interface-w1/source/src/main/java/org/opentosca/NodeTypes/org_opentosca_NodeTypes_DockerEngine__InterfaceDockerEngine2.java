
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
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
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.vngx.jsch.Session;
import ssh.SSHRemoteFileTransfer;
import ssh.SSHSessionFactory;

//@WebService(targetNamespace = "http://NodeTypes.opentosca.org/")
public class org_opentosca_NodeTypes_DockerEngine__InterfaceDockerEngine2 extends AbstractIAService {

    @WebMethod
    @SOAPBinding
    @Oneway
    public void startContainer(
            @WebParam(name = "DockerEngineURL", targetNamespace = "http://NodeTypes.opentosca.org/") final String dockerEngineURL,
            @WebParam(name = "DockerEngineCertificate", targetNamespace = "http://NodeTypes.opentosca.org/") final String dockerEngineCertificate,
            @WebParam(name = "ContainerImage", targetNamespace = "http://NodeTypes.opentosca.org/") final String containerImage,
            @WebParam(name = "ContainerPorts", targetNamespace = "http://NodeTypes.opentosca.org/") final String containerPorts,
            @WebParam(name = "ContainerEnv", targetNamespace = "http://NodeTypes.opentosca.org/") final String containerEnv,
            @WebParam(name = "ImageLocation", targetNamespace = "http://NodeTypes.opentosca.org/") final String imageLocation,
            @WebParam(name = "PrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") final String privateKey,
            @WebParam(name = "Links", targetNamespace = "http://NodeTypes.opentosca.org/") final String links,
            @WebParam(name = "Devices", targetNamespace = "http://NodeTypes.opentosca.org/") final String devices,
            @WebParam(name = "RemoteVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") final String remoteVolumeData,
            @WebParam(name = "HostVolumeData", targetNamespace = "http://NodeTypes.opentosca.org/") final String hostVolumeData,
            @WebParam(name = "ContainerMountPath", targetNamespace = "http://NodeTypes.opentosca.org/") final String containerMountPath,
            @WebParam(name = "VMIP", targetNamespace = "http://NodeTypes.opentosca.org/") final String vmIp,
            @WebParam(name = "VMPrivateKey", targetNamespace = "http://NodeTypes.opentosca.org/") final String vmPrivateKey) {
        // create connection to the docker engine
        DockerClient dockerClient = getDockerClient(dockerEngineURL, dockerEngineCertificate);

        // cut ip address out of DockerEngineURL
        final String ipAddress = dockerEngineURL.split(":")[1].substring(2);

        System.out.println("Try to connect to " + ipAddress);

        // create image or pull it if a remote image shall be used
        String image = null;
        if (containerImage == null) { // either ContainerImage or ImageLocation has to be set
            image = "da/" + System.currentTimeMillis();
            File basePath = new File(imageLocation);

            try {
                final URI dockerImageURI = new URI(imageLocation);

                final String[] pathSplit = dockerImageURI.getRawPath().split("/");
                final String fileName = pathSplit[pathSplit.length - 1];

                if (dockerImageURI.isAbsolute() | new File(dockerImageURI.toString()).exists()) {
                    final Path tempDir = Files.createTempDirectory("zip");
                    final Path tempUnpackDir = Files.createTempDirectory("unpacked");
                    File tempFile = new File(tempDir.toString(), fileName);

                    if (dockerImageURI.toString().startsWith("http")) {

                        download(dockerImageURI, tempFile);
                    } else {
                        tempFile = basePath;
                    }

                    if (fileName.endsWith("zip")) {

                        final ZipFile zipFile = new ZipFile(tempFile);

                        zipFile.extractAll(tempUnpackDir.toString());

                        basePath = new File(tempUnpackDir.toString(), "Dockerfile");
                        System.out.println(
                                "Unpacked DockerContainer Files, base Dockerfile at " + basePath.getAbsolutePath());
                    } else if (fileName.endsWith("tar.gz")) {
                        basePath = tempFile;
                        // open tarbal and look into repository file for the image tag

                        final TarArchiveInputStream tarIn = new TarArchiveInputStream(
                                new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))));

                        TarArchiveEntry entry = null;

                        while ((entry = tarIn.getNextTarEntry()) != null) {
                            if (entry.getName().trim().equals("repositories")) {
                                final File entryFile = new File(tempDir.toString(), entry.getName());

                                final OutputStream out = new FileOutputStream(entryFile);

                                IOUtils.copy(tarIn, out);
                                out.close();

                                final String repositoryContents = FileUtils.readFileToString(entryFile);

                                final ObjectMapper objMapper = new ObjectMapper();

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

                        tarIn.close();
                    }
                }
            } catch (final URISyntaxException | IOException | ZipException e) {
                e.printStackTrace();
            }

            if (basePath.getName().contains("Dockerfile")) {
                final BuildImageResultCallback callback = new BuildImageResultCallback() {
                    @Override
                    public void onNext(final BuildResponseItem item) {
                        System.out.println("" + item);
                        super.onNext(item);
                    }
                };

                System.out.println("Starting to build image from zip file at " + basePath);
                dockerClient.buildImageCmd(basePath).withTag(image).exec(callback).awaitImageId();

                try {
                    callback.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } else {
                // the tar.gz case
                if (isImageAvailable(image, dockerClient) == null) {
                    System.out.println("Image " + image + " not found");
                    System.out.println("Starting to load image from tar.gz file at " + basePath);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(basePath);
                        dockerClient.loadImageCmd(fis).exec();
                    } catch (final FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Image " + image + " already available skipping upload");
                }
            }
        } else {
            image = isImageAvailable(containerImage, dockerClient);
            if (image == null) {
                System.out.println("App container image not yet available. Pulling image.");
                dockerClient.pullImageCmd(containerImage)
                        .exec(new PullImageResultCallback())
                        .awaitSuccess();
                image = containerImage;
            }
        }

        // expose ports if needed for the container
        final List<ExposedPort> exposedPorts = new ArrayList<>();
        Ports portBindings = new Ports();
        if (containerPorts != null) {
            for (final String portMapping : containerPorts.split(";")) {
                if (portMapping.trim().isEmpty()) {
                    continue;
                }
                final String[] portMapKV = portMapping.split(",");
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
                    System.out.println("Creating PortBinding " + tempPort + ":" + externalPort);
                    portBindings.bind(tempPort, Ports.Binding.bindPort(externalPort));
                } else {
                    // map to random port
                    portBindings.bind(tempPort, Ports.Binding.empty());
                }
            }
        }

        // parse environment variables
        List<String> environmentVariables = new ArrayList<>();
        if (containerEnv != null) {
            environmentVariables = Arrays.asList(containerEnv.split(";"));
        }

        System.out.println("Will start container with following environment variables: ");
        System.out.println(environmentVariables.toString());

        final List<Link> linkList = new ArrayList<>();
        if (links != null) {
            final String[] idsToLinkSplit = links.split(";");
            for (final String idToLink : idsToLinkSplit) {
                System.out.println("Will linking container to container with id " + idToLink);
                linkList.add(new Link(idToLink.trim(), null));
            }
        }

        final List<Device> deviceList = new ArrayList<>();
        if (devices != null) {
            final String[] devMappingSplit = devices.split(";");
            for (final String devMapping : devMappingSplit) {
                final String[] devMapSplit = devMapping.split("=");
                if (devMapSplit.length == 2) {
                    System.out.println("Will add device " + devMapSplit[0] + ":" + devMapSplit[1]);
                    deviceList.add(new Device("mrw", devMapSplit[0], devMapSplit[1]));
                }
            }
        }

        final String hostVolPath = "/volumeFor" + image.replace("/", "_").replace(":", "") + System.currentTimeMillis();
        Volume volume = mountContainerVolume(dockerClient, containerMountPath, remoteVolumeData, hostVolumeData, hostVolPath, vmIp, vmPrivateKey, image);

        CreateContainerResponse container = null;
        if (volume != null && !containerMountPath.isEmpty()) {
            container = dockerClient.createContainerCmd(image)
                    .withEnv(environmentVariables)
                    .withTty(true)
                    .withLinks(linkList)
                    .withExposedPorts(exposedPorts)
                    .withPortBindings(portBindings)
                    .withBinds(new Bind(hostVolPath, volume))
                    .withDevices(deviceList)
                    .withCmd("-v")
                    .exec();
        } else {
            // start container
            container = dockerClient.createContainerCmd(image)
                    .withExposedPorts(exposedPorts)
                    .withPortBindings(portBindings)
                    .withEnv(environmentVariables)
                    .withTty(true)
                    .withLinks(linkList)
                    .withDevices(deviceList)
                    .exec();
        }

        System.out.println("Created container " + container.getId());
        dockerClient.startContainerCmd(container.getId()).exec();
        System.out.println("Started container " + container.getId());

        // get name of the new container
        final String containerName = dockerClient.inspectContainerCmd(container.getId())
                .exec()
                .getName().substring(1);

        StringBuilder portMapping = new StringBuilder();
        boolean first = true;
        for (final ExposedPort port : exposedPorts) {
            if (!first) {
                portMapping.append(",");
            }
            portMapping.append(port.getPort()).append("-->").append(findPort(dockerClient, container.getId(), port.getPort()));
            first = false;
        }

        // this HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();
        returnParameters.put("ContainerPorts", portMapping.toString());
        returnParameters.put("ContainerID", container.getId());
        returnParameters.put("ContainerIP", ipAddress);

        try {
            dockerClient.close();
        } catch (final Exception e) {
            System.out.println("Error while closing docker client.");
        }

        sendResponse(returnParameters);
    }

    private DockerClient getDockerClient(String dockerEngineURL, String dockerEngineCertificate) {
        DefaultDockerClientConfig config;
        if (dockerEngineCertificate == null || dockerEngineCertificate.isEmpty()) {
            config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerEngineURL)
                    .withDockerTlsVerify(false)
                    .build();
        } else {
            config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerEngineURL)
                    .withDockerCertPath(dockerEngineCertificate)
                    .build();
        }

        System.out.println(config.toString());

        return DockerClientBuilder.getInstance(config).build();
    }

    private Volume mountContainerVolume(DockerClient dockerClient, String ContainerMountPath, String RemoteVolumeData, String HostVolumeData, String hostVolPath,
                                        String VMIP, String VMPrivateKey, String image) {
        // FIXME It is important to notice that here the bindings are reversed! Which
        // at least I think is a bug in the java sdk!
        // <groupId>com.github.docker-java</groupId>
        // <artifactId>docker-java</artifactId>
        // <version>3.0.10</version>
        if (ContainerMountPath != null && !ContainerMountPath.isEmpty()) {
            final boolean pullResVol = pullImage(dockerClient, "phusion/baseimage:latest");
            // CreateVolumeResponse volResp = dockerClient.createVolumeCmd()
            // .withName().exec();

            Volume volume = new Volume(ContainerMountPath);

            final CreateContainerResponse volumeContainer = dockerClient.createContainerCmd("phusion/baseimage:latest")
                    .withBinds(new Bind(hostVolPath, volume, AccessMode.rw)).withVolumes(volume).exec();
            System.out.println("Created volume container " + volumeContainer.getId());
            dockerClient.startContainerCmd(volumeContainer.getId()).exec();
            System.out.println("Started volume container " + volumeContainer.getId());
            try {
                final ExecCreateCmdResponse execCmdResp = dockerClient.execCreateCmd(volumeContainer.getId())
                        .withCmd("mkdir", "-p", ContainerMountPath).exec();
                dockerClient.execStartCmd(execCmdResp.getId()).exec(new ExecStartResultCallback(System.out, System.err))
                        .awaitCompletion();

                if (RemoteVolumeData != null) {

                    // volumeData is a set of http paths pointing to tar files
                    final String[] dataPaths = RemoteVolumeData.split(";");

                    for (final String dataPath : dataPaths) {
                        final File volumeFile = downloadFile(dataPath);

                        final File volumeTarFile = createTempTarFromFile(volumeFile);

                        dockerClient.copyArchiveToContainerCmd(volumeContainer.getId())
                                .withRemotePath(ContainerMountPath)
                                .withTarInputStream(new FileInputStream(volumeTarFile)).exec();

                        final ExecCreateCmdResponse execChmodCmdResp = dockerClient
                                .execCreateCmd(volumeContainer.getId())
                                .withCmd("chmod", "600", ContainerMountPath + "/" + volumeFile.getName()).exec();
                        dockerClient.execStartCmd(execChmodCmdResp.getId())
                                .exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
                    }
                }

                if (HostVolumeData != null && VMIP != null && VMPrivateKey != null) {
                    final String[] dataPaths = HostVolumeData.split(";");

                    for (final String dataPath : dataPaths) {

                        final File tempFile = downloadFileFromSFTP(dataPath, VMIP, VMPrivateKey);

                        dockerClient.copyArchiveToContainerCmd(volumeContainer.getId())
                                .withHostResource(tempFile.getAbsolutePath())
                                .withRemotePath(ContainerMountPath)
                                .exec();

                        final ExecCreateCmdResponse execRenameCmdResp = dockerClient
                                .execCreateCmd(volumeContainer.getId())
                                .withCmd("mv", ContainerMountPath + "/" + tempFile.getName(),
                                        ContainerMountPath + "/" + new File(dataPath).getName())
                                .exec();
                        dockerClient.execStartCmd(execRenameCmdResp.getId())
                                .exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();

                        final ExecCreateCmdResponse execChmodCmdResp = dockerClient
                                .execCreateCmd(volumeContainer.getId())
                                .withCmd("chmod", "600", ContainerMountPath + "/" + new File(dataPath).getName())
                                .exec();
                        dockerClient.execStartCmd(execChmodCmdResp.getId())
                                .exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
                    }
                }
            } catch (final IOException | InterruptedException e) {
                e.printStackTrace();
            }

            return volume;
        }

        return null;
    }

    private File downloadFile(final String url) {
        try {
            final URI dockerImageURI = new URI(url);

            final String[] pathSplit = dockerImageURI.getRawPath().split("/");
            final String fileName = pathSplit[pathSplit.length - 1];

            final Path tempDir = Files.createTempDirectory(fileName);
            final File tempFile = new File(tempDir.toString(), fileName);

            download(dockerImageURI, tempFile);
            return tempFile;
        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void download(URI dockerImageURI, File tempFile) throws IOException {
        final URLConnection connection = dockerImageURI.toURL().openConnection();
        connection.setRequestProperty("Accept", "application/octet-stream");

        final InputStream input = connection.getInputStream();
        final byte[] buffer = new byte[4096];
        int n = -1;

        final OutputStream output = new FileOutputStream(tempFile);
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
        output.close();
        input.close();
    }

    private File downloadFileFromSFTP(final String filePath, final String vmip, final String vmprivateKey) {
        final Session session = SSHSessionFactory.createSSHSession(vmip.trim(), "ubuntu", vmprivateKey.trim());

        final SSHRemoteFileTransfer fileTransf = new SSHRemoteFileTransfer(session);

        try {
            final File tempFile = File.createTempFile(new File(filePath).getName(), "");

            fileTransf.getFile(filePath, tempFile);

            return tempFile;
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
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
     * @param image        the image to check availability for
     * @param dockerClient the client connected to the host
     * @return The image ID if the image is available, null otherwise
     */
    private String isImageAvailable(final String image, final DockerClient dockerClient) {
        System.out.println("Searching available Images: ");
        for (final Image availImage : dockerClient.listImagesCmd().exec()) {
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
            @WebParam(name = "ContainerID", targetNamespace = "http://NodeTypes.opentosca.org/") final String containerID) {
        // create connection to the docker engine
        final DockerClient dockerClient = getDockerClient(DockerEngineURL, DockerEngineCertificate);

        // stop ssh and real container together
//        for (final String id : ContainerID.split(";")) {
        // stop and remove container
        dockerClient.stopContainerCmd(containerID).exec();
        dockerClient.removeContainerCmd(containerID).exec();
//        }

        // this HashMap holds the return parameters of this operation.
        final HashMap<String, String> returnParameters = new HashMap<>();

        returnParameters.put("Result", "Stopped and Removed container " + containerID);

        try {
            dockerClient.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(10000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        sendResponse(returnParameters);
    }

    private boolean pullImage(final DockerClient dockerClient, final String imageName) {
        // pull needed image if not already done
        try {
            System.out.println("Fetching container image " + imageName);
            dockerClient.pullImageCmd(imageName)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            return true;
        } catch (final Exception e) {
            System.out.println("Couldnt connect, thus, wait and retry.");
            e.printStackTrace();
            try {
                Thread.sleep(2500);
                dockerClient.pullImageCmd(imageName)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion();
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
                return false;
            }
        }
        return true;
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
                    if (port.getPrivatePort() == searchedPort) {
                        return port.getPublicPort();
                    }
                }
            }
        }
        return -1;
    }
}
