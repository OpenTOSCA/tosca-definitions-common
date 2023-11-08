package org.opentosca.artifacttemplates.ec2;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
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

            // check availability of given instance type
            logger.info("Retrieving InstanceType for given VMType: {}", request.getVMType());
            InstanceType instanceType = InstanceType.fromValue(request.getVMType());
            logger.info("Successfully retrieved InstanceType: {}", instanceType);

            // if VM security group is not set, generate new security group
            String securityGroupId = null;
            if (request.getVMSecurityGroup() == null) {
                // generate security group for the VM
                logger.info("Generating new security group for the VM...");
                CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
                csgr.withGroupName("OpenTOSCA-" + System.currentTimeMillis()).withDescription("Auto generated security group for OpenTOSCA");
                CreateSecurityGroupResult createSecurityGroupResult = ec2Client.createSecurityGroup(csgr);
                securityGroupId = createSecurityGroupResult.getGroupId();
                logger.info("Generated new security group with ID: {}", securityGroupId);

                // enable ssh access from any IP
                logger.info("Enabling SSH access to the VM via port 22...");
                IpPermission ipPermission = new IpPermission();
                IpRange ipRange = new IpRange().withCidrIp("0.0.0.0/0");
                ipPermission.withIpv4Ranges(Collections.singletonList(ipRange))
                        .withIpProtocol("tcp")
                        .withFromPort(22)
                        .withToPort(22);
                AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
                authorizeSecurityGroupIngressRequest.withGroupId(securityGroupId).withIpPermissions(ipPermission);
                ec2Client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
                logger.info("Successfully enabled SSH access!");

                // TODO: open further app ports
            } else {
                // search for matching security group
                logger.info("Searching for security group with given Id: {}", request.getVMSecurityGroup());
                DescribeSecurityGroupsResult securityDescription = ec2Client.describeSecurityGroups(new DescribeSecurityGroupsRequest());
                List<SecurityGroup> securityGroups = securityDescription.getSecurityGroups();
                logger.info("Found {} security groups!", securityGroups.size());

                // abort if security group with given name does not exist
                if (securityGroups.stream().noneMatch(x -> x.getGroupId().equals(request.getVMSecurityGroup()))) {
                    logger.error("Unable to find security group with name {} in given AWS region: {}", request.getVMSecurityGroup(), request.getAWSREGION());
                    response.setError("Unable to find security group with the following ID in given AWS region: " + request.getVMSecurityGroup());
                    SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                    return;
                }
                logger.info("Found matching security group!");
                securityGroupId = request.getVMSecurityGroup();
            }

            // check if key pair with given name is available
            logger.info("Checking if key pair with name {} exists...", request.getVMKeyPairName());
            List<KeyPairInfo> keyPairs = ec2Client.describeKeyPairs().getKeyPairs();
            logger.info("Found {} key pairs!", keyPairs.size());
            if (keyPairs.stream().noneMatch(x -> x.getKeyName().equals(request.getVMKeyPairName()))) {
                logger.error("Unable to find key pair with name {} in given AWS region: {}", request.getVMKeyPairName(), request.getAWSREGION());
                response.setError("Unable to find key pair with the following ID in given AWS region: " + request.getVMKeyPairName());
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                return;
            }

            // create request for VM creation
            logger.info("Creating request for VM startup...");
            RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
            runInstancesRequest.withImageId(request.getVMImageID())
                    .withInstanceType(instanceType)
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName(request.getVMKeyPairName())
                    .withSecurityGroupIds(securityGroupId);

            logger.info("Starting VM...");
            RunInstancesResult result = ec2Client.runInstances(runInstancesRequest);
            Instance instance = result.getReservation().getInstances().get(0);
            logger.info("VM started with ID: {}", instance.getInstanceId());
            Thread.sleep(5000);

            // wait for the VM to start
            String state = "undefined";
            int iteration = 0;
            while (!state.equals("running") && iteration <= 50) {
                logger.info("Waiting for VM to start... Iteration: {}", iteration);

                // get current state of the created VM
                InstanceState instanceState = getCurrentInstance(ec2Client, instance.getInstanceId()).getState();
                state = instanceState.getName();
                logger.info("Current state: {}", instanceState.getName());

                Thread.sleep(15000);
                iteration++;
            }

            // abort if VM did not enter running state
            if (!state.equals("running")){
                logger.error("VM did not enter running state before timeout. Current state: {}", state);
                response.setError("VM did not enter running state before timeout. Current state: " + state);
                SoapUtil.sendSoapResponse(response, InvokeResponse.class, openToscaHeaders.replyTo());
                return;
            }
            logger.info("VM successfully started...");

            // get state of started VM
            instance = getCurrentInstance(ec2Client, instance.getInstanceId());
            logger.info("VM instance: {}", instance.toString());
            String publicIp = instance.getPublicIpAddress();
            logger.info("Public IP of VM: {}", publicIp);

            // Send response with instance ID and public IP address
            response.setVMInstanceID(instance.getInstanceId());
            response.setVMIP(publicIp);
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

    private Instance getCurrentInstance(AmazonEC2 ec2Client, String instanceId){
        DescribeInstancesRequest instanceStateRequest = new DescribeInstancesRequest();
        instanceStateRequest.setInstanceIds(Collections.singleton(instanceId));
        DescribeInstancesResult instanceStateResult = ec2Client.describeInstances(instanceStateRequest);
        return instanceStateResult.getReservations().get(0).getInstances().get(0);
    }
}
