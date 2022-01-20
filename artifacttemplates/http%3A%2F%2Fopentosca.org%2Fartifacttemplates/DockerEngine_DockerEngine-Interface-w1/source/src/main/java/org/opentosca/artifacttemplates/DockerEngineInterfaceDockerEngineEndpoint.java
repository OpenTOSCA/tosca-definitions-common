package org.opentosca.artifacttemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import org.opentosca.nodetypes.RemoveContainerRequest;
import org.opentosca.nodetypes.RemoveContainerResponse;
import org.opentosca.nodetypes.StartContainerRequest;
import org.opentosca.nodetypes.StartContainerResponse;

@Endpoint
public class DockerEngineInterfaceDockerEngineEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(DockerEngineInterfaceDockerEngineEndpoint.class);

    @PayloadRoot(namespace = Constants.NAMESPACE_URI, localPart = "startContainerRequest")
    @ResponsePayload
    public StartContainerResponse startContainer(@RequestPayload StartContainerRequest request) {
        LOG.info("Received startContainer request!");

        // TODO
        LOG.info(String.valueOf(request));

        return new StartContainerResponse();
    }

    @PayloadRoot(namespace = Constants.NAMESPACE_URI, localPart = "removeContainerRequest")
    @ResponsePayload
    public RemoveContainerResponse removeContainer(@RequestPayload RemoveContainerRequest request) {
        LOG.info("Received removeContainer request!");

        // TODO
        LOG.info(String.valueOf(request));

        return new RemoveContainerResponse();
    }
}
