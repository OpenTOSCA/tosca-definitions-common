package org.opentosca.artifacttemplates.ec2;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
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

        logger.info("Parsing OpenTOSCA headers...");
        OpenToscaHeaders openToscaHeaders = SoapUtil.parseHeaders(messageContext);
        InvokeResponse response = new InvokeResponse();
        response.setMessageID(openToscaHeaders.messageId());
        logger.info("Extracted message ID from header: {}", openToscaHeaders.messageId());

        // create client to interact with AWS
        AWSCredentials awsCreds = new BasicAWSCredentials(request.getAWSACCESSKEYID(), request.getAWSSECRETACCESSKEY());
        AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(request.getAWSREGION())
                .build();
        logger.info("Created client to access EC2...");

        try {
            // get available images and filter with given image ID
            logger.info("Searching for VM image with following ID: {}", request.getVMImageID());
            DescribeImagesRequest amiRequest = new DescribeImagesRequest().withFilters(new LinkedList<>());
            amiRequest.getFilters().add(new Filter().withName("image-id").withValues(request.getVMImageID()));
            List<Image> images = ec2Client.describeImages(amiRequest).getImages();
            logger.info("Found {} images with the given ID!", images.size());
            if (images.isEmpty()) {
                logger.error("Unable to find VM image with ID {} in given AWS region: {}", request.getVMImageID(), request.getAWSREGION());
                response.setError("Unable to find VM image with the following ID in given AWS region: " + request.getVMImageID());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                return;
            }

            // create request for VM creation
            logger.info("Creating request for VM startup...");
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
            runInstancesRequest.withImageId(request.getVMImageID())
                    .withInstanceType(InstanceType.T1Micro) // TODO
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName(request.getVMKeyPairName())
                    .withSecurityGroups("my-security-group"); // TODO

            logger.info("Starting VM...");
            RunInstancesResult result = ec2Client.runInstances(runInstancesRequest);

            // TODO: create VM
            String ip = "test";
            String vmInstanceId = "ID";

            // Output Parameters
            response.setVMInstanceID(vmInstanceId);
            response.setVMIP(ip);

            logger.info("Successfully started VM with public IP {}", ip);

            SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
        } catch (Exception e) {
            logger.error("Unable to create VM: {}", e.getMessage());
            response.setError("Unable to create VM: " + e.getMessage());
            SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
        }
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
