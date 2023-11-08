package org.opentosca.artifacttemplates.ec2;
import org.opentosca.artifacttemplates.OpenToscaHeaders;
import org.opentosca.artifacttemplates.SoapUtil;
import org.opentosca.artifacttemplates.ec2.model.CreateVMRequest;
import org.opentosca.artifacttemplates.ec2.model.InvokeResponse;
import org.opentosca.artifacttemplates.ec2.model.TerminateVMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

@Endpoint
public class EC2CloudProviderInterfaceEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(EC2CloudProviderInterfaceEndpoint.class);

    @PayloadRoot(namespace = EC2Constants.NAMESPACE_URI, localPart = "createVMRequest")
    public void createVM(@RequestPayload CreateVMRequest request, MessageContext messageContext) {
        logger.info("Received create VM request!");

        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());

        // TODO: create VM
        String ip = "test";
        String vmInstanceId = "ID";

        // Output Parameters
        response.setVMInstanceID(vmInstanceId);
        response.setVMIP(ip);

        logger.info("Successfully started VM with public IP {}", ip);

        SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
    }

    @PayloadRoot(namespace = EC2Constants.NAMESPACE_URI, localPart = "terminateVMRequest")
    public void terminateVM(@RequestPayload TerminateVMRequest request, MessageContext messageContext) {
        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);

        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());

        // TODO: terminate VM

        SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
    }
}
