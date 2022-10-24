package com.datahub.infra.apiaws.service.impl;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.amazonaws.services.pricing.model.FilterType;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.datahub.infra.apiaws.service.AwsService;
import com.datahub.infra.core.exception.CityHubUnAuthorizedException;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.model.ImageDetailInfo;
import com.datahub.infra.coreaws.model.*;
import com.datahub.infra.coreaws.model.KeyPairInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.ImageService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;


import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class AwsServiceImpl implements AwsService {

    @Autowired
    private ImageService imageService;

    private final static Logger logger = LoggerFactory.getLogger(AwsServiceImpl.class);

    private Ec2Client getEc2Client(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        try {
            Ec2Client ec2 = Ec2Client.builder().region(Region.of(info.getRegion())).build();
            return ec2;
        } catch (Exception e) {
            logger.error("Failed to validate credential : '{}'", e.getMessage());
            e.printStackTrace();
            throw new CityHubUnAuthorizedException(e.getMessage());
        }
    }

    private IamClient getIamClient(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build();

        return iam;
    }

    private CloudWatchClient getCloudWatchClient(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        CloudWatchClient cw = CloudWatchClient.builder().region(Region.of(info.getRegion())).build();

        return cw;
    }

    private RdsClient getRdsClient(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        RdsClient rds = RdsClient.builder().region(Region.of(info.getRegion())).build();

        return rds;
    }

    private S3Client getS3Client(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        S3Client s3 = S3Client.builder().region(Region.of(info.getRegion())).build();

        return s3;
    }

    private ElasticLoadBalancingV2Client getElbClient(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        ElasticLoadBalancingV2Client elb = ElasticLoadBalancingV2Client.builder().region(Region.of(info.getRegion())).build();

        return elb;
    }

    private CostExplorerClient getCostClient(CredentialInfo info) {

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());

        CostExplorerClient ce = CostExplorerClient.builder().region(Region.AWS_GLOBAL).build();

        return ce;
    }

    public boolean validateCredential(CredentialInfo info) {
        boolean isValid = true;

        String accessKey = System.getProperty("aws.accessKeyId");
        String secretAccessKey = System.getProperty("aws.secretAccessKey");

        System.setProperty("aws.accessKeyId", info.getAccessId());
        System.setProperty("aws.secretAccessKey", info.getAccessToken());
        Ec2Client ec2 = Ec2Client.builder().region(Region.of(info.getRegion())).build();
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();

        try {
            DescribeInstancesResponse response = ec2.describeInstances(request);
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails().errorCode().equals("AuthFailure")) {
                isValid = false;
                System.setProperty("aws.accessKeyId", accessKey);
                System.setProperty("aws.secretAccessKey", secretAccessKey);
            }
            logger.error("Failed to validate credential : '{}'", e.getMessage());
        }

        return isValid;
    }

    public List<ServerInfo> getServers(CredentialInfo credentialInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);


        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        Boolean done = false;
        while(!done) {
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for(Reservation reservation : response.reservations()) {
                for(Instance instance : reservation.instances()) {
                    if(webCheck){
                        ServerInfo info = new ServerInfo(instance);
                        list.add(info);
                    }else{
                        ServerInfo2 info2 = new ServerInfo2(instance);
                        String os = getOsType(credentialInfo, instance.imageId());
                        FlavorInfo flavorInfo = getFlavor(credentialInfo, os, instance.instanceType().toString());
                        if(flavorInfo != null) {
                            String memory = flavorInfo.getMemory();
                            double doubleMemory = Double.parseDouble(memory.replaceAll("[^0-9.]", ""));
                            info2.setMemory(doubleMemory);
                        }
                        info2.setDisk(getVolumeSize(credentialInfo, info2.getId()));
                        list2.add(info2);
                    }
                }
            }
            if(response.nextToken() == null) {
                done = true;
            }
        }
        ec2.close();

        Map<String, Integer> count = new HashMap<>();

        for(ServerInfo2 temp : list2){
            if(count.containsKey(temp.getName())){
                count.put(temp.getName(), count.get(temp.getName()) + 1);
            }else{
                count.put(temp.getName(), 1);
            }
        }

        for(ServerInfo2 temp : list2){
            if(count.containsKey(temp.getName())){
                temp.setInstanceCount(count.get(temp.getName()));
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        if (webCheck) {
            return jsonArray;
        } else {
            return jsonArray2;
        }
    }

    public List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();

        List<ServerInfo2> list2 = new ArrayList<>();
        Boolean done = false;
        while(!done) {
            DescribeInstancesResponse response = ec2.describeInstances(request);

            for(Reservation reservation : response.reservations()) {
                for(Instance instance : reservation.instances()) {
                    ServerInfo2 info2 = new ServerInfo2(instance);
                    String os = getOsType(credentialInfo, instance.imageId());
                    FlavorInfo flavorInfo = getFlavor(credentialInfo, os, instance.instanceType().toString());
                    if(flavorInfo != null) {
                        String memory = flavorInfo.getMemory();
                        double doubleMemory = Double.parseDouble(memory.replaceAll("[^0-9.]", ""));
                        info2.setMemory(doubleMemory);
                    }
                    info2.setDisk(getVolumeSize(credentialInfo, info2.getId()));
                    if(type.equals("name")){
                        if(info2.getName().equals(value)) list2.add(info2);
                    }
                    if(type.equals("serverState")){
                        if(info2.getServerState().equals(value)) list2.add(info2);
                    }
                }
            }
            if(response.nextToken() == null) {
                done = true;
            }
        }
        ec2.close();

        Map<String, Integer> count = new HashMap<>();

        for(ServerInfo2 temp : list2){
            if(count.containsKey(temp.getName())){
                count.put(temp.getName(), count.get(temp.getName()) + 1);
            }else{
                count.put(temp.getName(), 1);
            }
        }

        for(ServerInfo2 temp : list2){
            if(count.containsKey(temp.getName())){
                temp.setInstanceCount(count.get(temp.getName()));
            }
        }

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }


    public List<ServerInfo> getServer(CredentialInfo credentialInfo, String id, Boolean webCheck) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(id).build();
        DescribeInstancesResponse response = ec2.describeInstances(request);
        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        ServerInfo info = new ServerInfo(response.reservations().get(0).instances().get(0));
        ServerInfo2 info2 = new ServerInfo2(response.reservations().get(0).instances().get(0));

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        if (webCheck) {
            for (int i = 0; i < info.getId().length(); i++) {

                if (info.getId() != null && info.getId().equals(id)) {
                    System.out.println("list0_aws_server = " + list);

                    list.add(info);
                    System.out.println("list1_aws_server = " + list);
                    ec2.close();
                    break;
                } else {
                    System.out.println("list2_aws_server = " + list);
                }
            }
            try {
                jsonString = mapper.writeValueAsString(list);
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            for (int i = 0; i < info.getId().length(); i++) {

                if (info2.getId() != null && info2.getId().equals(id)) {

                    String os = getOsType(credentialInfo, response.reservations().get(0).instances().get(0).imageId());
                    FlavorInfo flavorInfo = getFlavor(credentialInfo, os, response.reservations().get(0).instances().get(0).instanceType().toString());
                    if(flavorInfo != null){
                        String memory = flavorInfo.getMemory();
                        double doubleMemory = Double.parseDouble(memory.replaceAll("[^0-9.]", ""));
                        info2.setMemory(doubleMemory);
                    }

                    info2.setDisk(getVolumeSize(credentialInfo, info2.getId()));
                    list2.add(info2);

                    System.out.println("list1_aws_server = " + list2);
                    ec2.close();
                    break;

                } else {
                    System.out.println("list2_aws_server = " + list2);

                }
            }

            Map<String, Integer> count = new HashMap<>();

            for(ServerInfo2 temp : list2){
                if(count.containsKey(temp.getName())){
                    count.put(temp.getName(), count.get(temp.getName()) + 1);
                }else{
                    count.put(temp.getName(), 1);
                }
            }

            for(ServerInfo2 temp : list2){
                if(count.containsKey(temp.getName())){
                    temp.setInstanceCount(count.get(temp.getName()));
                }
            }

            try {
                jsonString2 = mapper.writeValueAsString(list2);
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        } else {
            return jsonArray2;
        }
    }

    public FlavorInfo getFlavor(CredentialInfo credentialInfo, String osType, String instanceType){
        FlavorInfo flavorInfo = null;
        List<FlavorInfo> flavorInfos = getFlavors(credentialInfo, osType);
        for(FlavorInfo temp : flavorInfos){
            if(temp.getInstanceType().equals(instanceType)){
                flavorInfo = temp;
            }
        }
        return flavorInfo;
    }

    public Object createServer(CredentialInfo credentialInfo, CreateServerInfo createServerInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if (webCheck) {
            if (createServerInfo.getScript() != null && !createServerInfo.getBase64Encoded()) {
                try {
                    createServerInfo.setScript(Base64.getEncoder().encodeToString(createServerInfo.getScript().getBytes("UTF-8")));
                } catch (UnsupportedEncodingException uee) {
                    logger.error("Failed to encode string to UTF-8 : '{}'", uee.getMessage());
                }
            }

            if (createServerInfo.getKeypair().equals("no")) {
                createServerInfo.setKeypair(null);
            }
            if (createServerInfo.getSubnetId().equals("Default Subnet")) {
                createServerInfo.setSubnetId(null);
            }

            RunInstancesRequest runInstancesRequest = RunInstancesRequest.builder()
                    .tagSpecifications(TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(Tag.builder().key("Name").value(createServerInfo.getName()).build()).build())
                    .imageId(createServerInfo.getImageId())
                    .instanceType(createServerInfo.getInstanceType())
                    .minCount(1)
                    .maxCount(createServerInfo.getInstanceCount())
                    .securityGroups(createServerInfo.getSecurityGroup())
                    .keyName(createServerInfo.getKeypair())
                    .monitoring(RunInstancesMonitoringEnabled.builder().enabled(createServerInfo.getMonitoringEnabled()).build())
                    .subnetId(createServerInfo.getSubnetId())
                    .userData(createServerInfo.getScript())
                    .build();

            RunInstancesResponse run_response = ec2.runInstances(runInstancesRequest);

            String reservation_id = run_response.reservationId();
            ServerInfo serverInfo = new ServerInfo(run_response.instances().get(0));
            list.add(serverInfo);

            return list;
        } else {
            if (createServerInfo.getScript() != null && !createServerInfo.getBase64Encoded()) {
                try {
                    createServerInfo.setScript(Base64.getEncoder().encodeToString(createServerInfo.getScript().getBytes("UTF-8")));
                } catch (UnsupportedEncodingException uee) {
                    logger.error("Failed to encode string to UTF-8 : '{}'", uee.getMessage());
                }
            }

            if (createServerInfo.getKeypair().equals("no")) {
                createServerInfo.setKeypair(null);
            }
            if (createServerInfo.getSubnetName().equals("Default Subnet")) {
                createServerInfo.setSubnetName(null);
            }

            RunInstancesRequest runInstancesRequest = RunInstancesRequest.builder()
                    .tagSpecifications(TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(Tag.builder().key("Name").value(createServerInfo.getName()).build()).build())
                    .imageId(createServerInfo.getImageId())
                    .instanceType(createServerInfo.getFlavorName())
                    .minCount(1)
                    .maxCount(createServerInfo.getInstanceCount())
                    .securityGroups(createServerInfo.getSecurityGroupName())
                    .keyName(createServerInfo.getKeypair())
                    .monitoring(RunInstancesMonitoringEnabled.builder().enabled(createServerInfo.getMonitoringEnabled()).build())
                    .subnetId(createServerInfo.getSubnetName())
                    .userData(createServerInfo.getScript())
                    .build();

            RunInstancesResponse run_response = ec2.runInstances(runInstancesRequest);

            String reservation_id = run_response.reservationId();
            ServerInfo2 serverInfo2 = new ServerInfo2(run_response.instances().get(0));
            String os = getOsType(credentialInfo, run_response.instances().get(0).imageId());
            FlavorInfo flavorInfo = getFlavor(credentialInfo, os, run_response.instances().get(0).instanceType().toString());
            if(flavorInfo != null){
                String memory = flavorInfo.getMemory();
                double doubleMemory = Double.parseDouble(memory.replaceAll("[^0-9.]", ""));
                serverInfo2.setMemory(doubleMemory);
            }

            serverInfo2.setDisk(getVolumeSize(credentialInfo, serverInfo2.getId()));

            list2.add(serverInfo2);
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }

    }

    @Override
    public ServerInfo start(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        InstanceStateChange stateChange;

        StartInstancesRequest request = StartInstancesRequest.builder().instanceIds(serverId).build();
        StartInstancesResponse response = ec2.startInstances(request);
        stateChange = response.startingInstances().get(0);

        ServerInfo serverInfo = new ServerInfo();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
        DescribeInstancesResponse res = ec2.describeInstances(req);
        serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));

        ec2.close();

        return serverInfo;
    }

    @Override
    public ServerInfo stop(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        InstanceStateChange stateChange;

        StopInstancesRequest request = StopInstancesRequest.builder().instanceIds(serverId).build();
        StopInstancesResponse response = ec2.stopInstances(request);
        stateChange = response.stoppingInstances().get(0);

        ServerInfo serverInfo = new ServerInfo();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
        DescribeInstancesResponse res = ec2.describeInstances(req);
        serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));

        ec2.close();

        return serverInfo;
    }

    @Override
    public ServerInfo reboot(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        InstanceStateChange stateChange;

        RebootInstancesRequest request = RebootInstancesRequest.builder().instanceIds(serverId).build();
        RebootInstancesResponse response = ec2.rebootInstances(request);

        ServerInfo serverInfo = new ServerInfo();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
        DescribeInstancesResponse res = ec2.describeInstances(req);
        serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));

        ec2.close();

        return serverInfo;
    }


    @Override
    public Object delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        DeleteInfo deleteInfo = new DeleteInfo();
        List<ServerInfo> list = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        InstanceStateChange stateChange;

        if (webCheck) {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder().instanceIds(serverId).build();
            TerminateInstancesResponse response = ec2.terminateInstances(request);
            stateChange = response.terminatingInstances().get(0);

            ServerInfo serverInfo = new ServerInfo();

            DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
            DescribeInstancesResponse res = ec2.describeInstances(req);
            serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));
            list.add(serverInfo);

            ec2.close();
            return list;
        }else{
            TerminateInstancesRequest request = TerminateInstancesRequest.builder().instanceIds(serverId).build();
            TerminateInstancesResponse response = ec2.terminateInstances(request);
            stateChange = response.terminatingInstances().get(0);

            DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
            DescribeInstancesResponse res = ec2.describeInstances(req);
            deleteInfo.setId(res.reservations().get(0).instances().get(0).instanceId());

            ec2.close();
        }
        try {
            jsonString = mapper.writeValueAsString(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }
    }

    @Override
    public ServerInfo monitoring(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        InstanceStateChange stateChange;

        MonitorInstancesRequest request = MonitorInstancesRequest.builder().instanceIds(serverId).build();
        MonitorInstancesResponse response = ec2.monitorInstances(request);

        ServerInfo serverInfo = new ServerInfo();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
        DescribeInstancesResponse res = ec2.describeInstances(req);
        serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));

        ec2.close();

        return serverInfo;
    }

    @Override
    public ServerInfo unmonitoring(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        InstanceStateChange stateChange;

        UnmonitorInstancesRequest request = UnmonitorInstancesRequest.builder().instanceIds(serverId).build();
        UnmonitorInstancesResponse response = ec2.unmonitorInstances(request);

        ServerInfo serverInfo = new ServerInfo();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(serverId).build();
        DescribeInstancesResponse res = ec2.describeInstances(req);
        serverInfo = new ServerInfo(res.reservations().get(0).instances().get(0));

        ec2.close();

        return serverInfo;
    }

    public List<RegionInfo> getRegions(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeRegionsResponse response = ec2.describeRegions();

        List<software.amazon.awssdk.services.ec2.model.Region> Regions = response.regions();

        List<RegionInfo> list = new ArrayList<>();

        for (software.amazon.awssdk.services.ec2.model.Region region : Regions) {
            RegionInfo info = new RegionInfo(region);
            System.out.printf(
                    "Found region %s " +
                            "with endpoint %s",
                    region.regionName(),
                    region.endpoint());
            System.out.println();
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<ZoneInfo> getAvailabilityZones(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeAvailabilityZonesResponse response = ec2.describeAvailabilityZones();

        List<AvailabilityZone> availabilityZones = response.availabilityZones();

        List<ZoneInfo> list = new ArrayList<>();
        for (AvailabilityZone zones : availabilityZones) {
            ZoneInfo info = new ZoneInfo(zones);
            System.out.printf(
                    "Found availability zone %s " +
                            "with status %s " +
                            "in region %s",
                    zones.zoneName(),
                    zones.state(),
                    zones.regionName());
            System.out.println();
            list.add(info);
        }

        ec2.close();
        logger.debug("zones return value -- list : {}", list);
        return list;
    }

    @Override
    public Object createVolume(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;

        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if (webCheck) {
            CreateVolumeResponse response;
            String volumeType = createData.get("volumeType").toString();
            String AvailabilityZone = createData.get("AvailabilityZone").toString();
            String Encrypted = createData.get("Encrypted").toString();
            String IOPS = createData.get("IOPS").toString();
            String Size = createData.get("Size").toString();

            if (volumeType.equals("io1")) {
                CreateVolumeRequest request = CreateVolumeRequest.builder()
                        .availabilityZone(AvailabilityZone)
                        .encrypted(Encrypted != "0" ? true : false)
                        .iops(Integer.parseInt(IOPS))
                        .size(Integer.parseInt(Size))
                        .volumeType(volumeType)
                        .build();
                response = ec2.createVolume(request);
            } else {
                CreateVolumeRequest request = CreateVolumeRequest.builder()
                        .availabilityZone(AvailabilityZone)
                        .encrypted(Encrypted != "0" ? true : false)
                        .size(Integer.parseInt(Size))
                        .volumeType(volumeType)
                        .build();
                response = ec2.createVolume(request);
            }

            DescribeVolumesRequest req = DescribeVolumesRequest.builder().volumeIds(response.volumeId()).build();
            DescribeVolumesResponse res = ec2.describeVolumes(req);

            VolumeInfo info = new VolumeInfo(res.volumes().get(0));
            list.add(info);

            ec2.close();

            return list;
        }else{
            CreateVolumeResponse response;
            String type = createData.get("volumeType").toString();
            String region = createData.get("region").toString();
            String encrypted = createData.get("encrypted").toString();
            String IOPS = createData.get("IOPS").toString();
            String size = createData.get("volumeSize").toString();

            if (type.equals("io1")) {
                CreateVolumeRequest request = CreateVolumeRequest.builder()
                        .availabilityZone(region)
                        .encrypted(encrypted != "0" ? true : false)
                        .iops(Integer.parseInt(IOPS))
                        .size(Integer.parseInt(size))
                        .volumeType(type)
                        .build();
                response = ec2.createVolume(request);
            } else {
                CreateVolumeRequest request = CreateVolumeRequest.builder()
                        .availabilityZone(region)
                        .encrypted(encrypted != "0" ? true : false)
                        .size(Integer.parseInt(size))
                        .volumeType(type)
                        .build();
                response = ec2.createVolume(request);
            }

            DescribeVolumesRequest req = DescribeVolumesRequest.builder().volumeIds(response.volumeId()).build();
            DescribeVolumesResponse res = ec2.describeVolumes(req);

            VolumeInfo2 info = new VolumeInfo2(res.volumes().get(0));
            list2.add(info);

            ec2.close();
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }
    }

    public List<VolumeInfo> getVolumes(CredentialInfo credentialInfo,Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();

        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();
        DescribeVolumesResponse response = ec2.describeVolumes(request);

        if(webCheck) {
            for (Volume volume : response.volumes()) {

                VolumeInfo info = new VolumeInfo(volume);
                list.add(info);
            }
        }
        else{
            for (Volume volume : response.volumes()) {

                VolumeInfo2 info = new VolumeInfo2(volume);
                list2.add(info);
            }
        }
        ec2.close();
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);


        if (webCheck) {
            return jsonArray;
        }
        else {
            return jsonArray2;
        }
    }

    public List<VolumeInfo> getVolumes_Search(CredentialInfo credentialInfo, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();

        List<VolumeInfo2> list2 = new ArrayList<>();
        DescribeVolumesResponse response = ec2.describeVolumes(request);

        for (Volume volume : response.volumes()) {
            VolumeInfo2 info2 = new VolumeInfo2(volume);
            if(type.equals("name")){
            }
            if(type.equals("volumeState")){
                if(info2.getState().equals(value)) list2.add(info2);
            }
        }

        ec2.close();

        try {
            jsonString2 = mapper.writeValueAsString(list2);

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }

    public List<VolumeInfo> getVolume(CredentialInfo credentialInfo, String id,Boolean webCheck) {

        if (credentialInfo == null) throw new CredentialException();
        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().volumeIds(id).build();
        DescribeVolumesResponse response = ec2.describeVolumes(request);

        VolumeInfo info = new VolumeInfo(response.volumes().get(0));
        VolumeInfo2 info2 = new VolumeInfo2(response.volumes().get(0));

        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();

        if(webCheck) {

            for (int i = 0; i < info.getId().length(); i++) {

                if (info.getId() != null && info.getId().equals(id)) {
                    System.out.println("list0_aws_voulme = " + list);

                    list.add(info);
                    System.out.println("list1_aws_volume = " + list);
                    ec2.close();
                    break;
                } else {
                    System.out.println("list2_aws_volume = " + list);

                }
            }
        }
        else{
            for (int i = 0; i < info2.getId().length(); i++) {

                if (info2.getId() != null && info2.getId().equals(id)) {
                    System.out.println("list0_aws_voulme = " + list2);

                    list2.add(info2);
                    System.out.println("list1_aws_volume = " + list2);
                    ec2.close();
                    break;
                } else {
                    System.out.println("list2_aws_volume = " + list2);

                }
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);

        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        if (webCheck) {
            return jsonArray;
        }
        else {
            return jsonArray2;
        }
    }

    public int getVolumeSize(CredentialInfo credentialInfo, String instanceId) {
        if (credentialInfo == null) throw new CredentialException();

        int volumeSize = 0;

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();
        DescribeVolumesResponse response = ec2.describeVolumes(request);

        for(Volume volume : response.volumes()) {
            VolumeInfo info = new VolumeInfo(volume);
            if(info.getInstanceId().equals(instanceId)){
                volumeSize = info.getSize();
            }
        }
        ec2.close();
        return volumeSize;
    }

    public Object deleteVolume(CredentialInfo credentialInfo, String volumeId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        DeleteInfo deleteInfo = new DeleteInfo();
        List<VolumeInfo> list = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if (webCheck) {
            DescribeVolumesRequest req = DescribeVolumesRequest.builder().volumeIds(volumeId).build();
            DescribeVolumesResponse res = ec2.describeVolumes(req);

            VolumeInfo volumeInfo = new VolumeInfo(res.volumes().get(0));

            DeleteVolumeRequest request = DeleteVolumeRequest.builder().volumeId(volumeId).build();
            DeleteVolumeResponse response = ec2.deleteVolume(request);

            list.add(volumeInfo);

            ec2.close();
            return list;
        }else{
            DescribeVolumesRequest req = DescribeVolumesRequest.builder().volumeIds(volumeId).build();
            DescribeVolumesResponse res = ec2.describeVolumes(req);

            deleteInfo.setId(res.volumes().get(0).volumeId());

            DeleteVolumeRequest request = DeleteVolumeRequest.builder().volumeId(volumeId).build();
            DeleteVolumeResponse response = ec2.deleteVolume(request);

            ec2.close();
        }
        try {
            jsonString = mapper.writeValueAsString(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }

    }

    public List<SnapshotInfo> getSnapshots(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        DescribeSnapshotsRequest request = DescribeSnapshotsRequest.builder().ownerIds("self").build();

        DescribeSnapshotsResponse response = ec2.describeSnapshots(request);

        List<SnapshotInfo> list = new ArrayList<>();
        for (Snapshot snapshot : response.snapshots()) {
            SnapshotInfo info = new SnapshotInfo(snapshot);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public String getOsType(CredentialInfo credentialInfo, String imageId){
        String osType = null;

        List<ImageDetailInfo> imageDetailInfos = imageService.getImageDetails("aws", null);
        for(ImageDetailInfo temp : imageDetailInfos){
            if(temp.getId().equals(imageId)){
                osType = temp.getOsType();
            }
        }
        if(osType == null){
            List<ImageInfo> imageInfos = getImages(credentialInfo);
            for(ImageInfo temp : imageInfos){
                if(temp.getId().equals(imageId)){
                    osType = temp.getOsType();
                }
            }
        }

        return osType;
    }
    public List<ImageInfo> getImages(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeImagesRequest request = DescribeImagesRequest.builder().owners("self").build();

        List<ImageInfo> list = new ArrayList<>();
        DescribeImagesResponse response = ec2.describeImages(request);

        for (Image image : response.images()) {
            ImageInfo info = new ImageInfo(image);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<KeyPairInfo> getKeyPairs(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeKeyPairsRequest request = DescribeKeyPairsRequest.builder().build();

        List<KeyPairInfo> list = new ArrayList<>();
        DescribeKeyPairsResponse response = ec2.describeKeyPairs(request);

        for (software.amazon.awssdk.services.ec2.model.KeyPairInfo keypair : response.keyPairs()) {
            KeyPairInfo info = new KeyPairInfo(keypair);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder().build();

        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        DescribeNetworkInterfacesResponse response = ec2.describeNetworkInterfaces(request);

        if (webCheck) {
            for (NetworkInterface networkInterface : response.networkInterfaces()) {

                NetworkInfo info = new NetworkInfo(networkInterface);

                list.add(info);
            }
            ec2.close();
        } else {
            for (NetworkInterface networkInterface : response.networkInterfaces()) {

                NetworkInfo2 info = new NetworkInfo2(networkInterface);

                list2.add(info);
            }
            ec2.close();
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        if (webCheck) {
            return jsonArray;
        } else {
            return jsonArray2;
        }
    }

    public List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder().build();

        List<NetworkInfo2> list2 = new ArrayList<>();
        DescribeNetworkInterfacesResponse response = ec2.describeNetworkInterfaces(request);
        for (NetworkInterface networkInterface : response.networkInterfaces()) {
            NetworkInfo2 info2 = new NetworkInfo2(networkInterface);
            if(type.equals("name")){
                if(info2.getName().equals(value)) list2.add(info2);
            }
            if(type.equals("networkState")){
                if(info2.getState().equals(value)) list2.add(info2);
            }
        }
        ec2.close();

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        return jsonArray2;
    }

    public List<SecurityGroupInfo> getSecurityGroups(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().build();

        List<SecurityGroupInfo> list = new ArrayList<>();
        DescribeSecurityGroupsResponse response = ec2.describeSecurityGroups(request);

        for (SecurityGroup securityGroup : response.securityGroups()) {
            SecurityGroupInfo info = new SecurityGroupInfo(securityGroup);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<AddressInfo> getAddresses(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeAddressesRequest request = DescribeAddressesRequest.builder().build();

        List<AddressInfo> list = new ArrayList<>();
        DescribeAddressesResponse response = ec2.describeAddresses(request);

        for (Address address : response.addresses()) {
            AddressInfo info = new AddressInfo(address);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<SubnetInfo> getSubnets(CredentialInfo credentialInfo, String vpcId) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder().build();

        List<SubnetInfo> list = new ArrayList<>();
        DescribeSubnetsResponse response = ec2.describeSubnets(request);

        for (Subnet subnet : response.subnets()) {
            if (vpcId.equals(subnet.vpcId())) {
                SubnetInfo info = new SubnetInfo(subnet);
                list.add(info);
            }
        }

        ec2.close();

        return list;
    }

    public List<VpcInfo> getVpcs(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeVpcsRequest request = DescribeVpcsRequest.builder().build();

        List<VpcInfo> list = new ArrayList<>();
        DescribeVpcsResponse response = ec2.describeVpcs(request);

        for (Vpc vpc : response.vpcs()) {
            VpcInfo info = new VpcInfo(vpc);
            list.add(info);
        }

        ec2.close();

        return list;
    }

    public List<UserInfo> getUsers(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        IamClient iam = getIamClient(credentialInfo);
        ListUsersRequest request = ListUsersRequest.builder().build();

        List<UserInfo> list = new ArrayList<>();
        ListUsersResponse response = iam.listUsers(request);

        for (User user : response.users()) {
            UserInfo info = new UserInfo(user);
            list.add(info);
        }

        iam.close();

        return list;
    }

    public List<GroupInfo> getGroups(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        IamClient iam = getIamClient(credentialInfo);
        ListGroupsRequest request = ListGroupsRequest.builder().build();

        List<GroupInfo> list = new ArrayList<>();
        ListGroupsResponse response = iam.listGroups(request);

        for (Group group : response.groups()) {
            GroupInfo info = new GroupInfo(group);
            list.add(info);
        }

        iam.close();

        return list;
    }

    public Map<String, Object> getServerMetric(CredentialInfo credentialInfo, RequestMetricInfo requestMetricInfo) {

        CloudWatchClient cw = getCloudWatchClient(credentialInfo);

        List<ServerMonitoringInfo> list = new ArrayList<>();

        Instant endTime = requestMetricInfo.getEndDate().toInstant();
        Instant startTime = requestMetricInfo.getStartDate().toInstant();
        Statistic statistics = Statistic.valueOf(requestMetricInfo.getStatistic());

        Dimension dimension = Dimension.builder().name(requestMetricInfo.getName()).value(requestMetricInfo.getId()).build();

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/EC2")
                .metricName(requestMetricInfo.getMetricName())
                .period(requestMetricInfo.getInterval())
                .statistics(statistics)
                .dimensions(dimension)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        GetMetricStatisticsResponse response = cw.getMetricStatistics(request);

        HashMap<String, Object> metricData = new HashMap<String, Object>();
        for (Datapoint datapoint : response.datapoints()) {
            ServerMonitoringInfo info = new ServerMonitoringInfo();
            info.setId(requestMetricInfo.getId());
            info.setStatistics(requestMetricInfo.getId());
            info.setPeriod(requestMetricInfo.getInterval());
            info.setMetricName(requestMetricInfo.getMetricName());
            info.setUnit(datapoint.unitAsString());
            info.setTimestamp(Timestamp.from(datapoint.timestamp()));

            if (statistics.equals(Statistic.AVERAGE)) {
                info.setValue(datapoint.average());
            } else if (statistics.equals(Statistic.MAXIMUM)) {
                info.setValue(datapoint.maximum());
            } else if (statistics.equals(Statistic.MINIMUM)) {
                info.setValue(datapoint.minimum());
            } else if (statistics.equals(Statistic.SUM)) {
                info.setValue(datapoint.sum());
            } else if (statistics.equals(Statistic.SAMPLE_COUNT)) {
                info.setValue(datapoint.sampleCount());
            }


            list.add(info);
        }

        List<Timestamp> time = new ArrayList<>();
        List<Double> value = new ArrayList<>();
        list.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
        for (ServerMonitoringInfo info : list) {
            time.add(info.getTimestamp());
            value.add(info.getValue());
        }

        switch (requestMetricInfo.getMetricName()) {
            case "CPUUtilization":
                metricData.put("CPUUtilizationCPU", value);
                break;
            case "DiskReadBytes":
                metricData.put("DiskBytesReads", value);
                break;
            case "DiskWriteBytes":
                metricData.put("DiskBytesWrites", value);
                break;
            case "DiskReadOps":
                metricData.put("DiskOpsRead", value);
                break;
            case "DiskWriteOps":
                metricData.put("DiskOpsWrite", value);
                break;
            case "NetworkIn":
                metricData.put("NetworkByteInput", value);
                break;
            case "NetworkOut":
                metricData.put("NetworkByteOutput", value);
                break;
            case "NetworkPacketsIn":
                metricData.put("NetworkPacketsIn Count", value);
                break;
            case "NetworkPacketsOut":
                metricData.put("NetworkPacketsOut Count", value);
                break;
            case "StatusCheckFailed":
                metricData.put("StatusCheckFailedAny", value);
                break;
            case "StatusCheckFailed_Instance":
                metricData.put("StatusCheckFailedInstance", value);
                break;
            case "StatusCheckFailed_System":
                metricData.put("StatusCheckFailedSystem", value);
                break;
            case "CPUCreditUsage":
                metricData.put("CPUCreditUsageCount", value);
                break;
            case "CPUCreditBalance":
                metricData.put("CPUCreditBalanceCount", value);
                break;
        }

        cw.close();

        return metricData;
    }

    public List<GroupInfo> imageTest(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        Filter filter = Filter.builder().name("a").values("aaa").build();
        DescribeImagesRequest request = DescribeImagesRequest.builder().filters(filter).build();

        List<GroupInfo> list = new ArrayList<>();
        DescribeImagesResponse response = ec2.describeImages(request);

        for (Image image : response.images()) {
        }
        ec2.close();

        return list;
    }

    public List<GroupInfo> resourceTest(CredentialInfo credentialInfo) {

        if (credentialInfo == null) throw new CredentialException();

        CloudWatchClient cw = getCloudWatchClient(credentialInfo);
        ListDashboardsRequest request = ListDashboardsRequest.builder().build();

        List<GroupInfo> list = new ArrayList<>();
        ListDashboardsResponse response = cw.listDashboards(request);

        for (DashboardEntry dashboardEntry : response.dashboardEntries()) {
        }
        cw.close();

        return list;
    }

    public List<GroupInfo> s3Test(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        S3Client s3 = getS3Client(credentialInfo);
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket("innogrid.cost").build();

        List<GroupInfo> list = new ArrayList<>();
        ListObjectsV2Response response = s3.listObjectsV2(request);

        for (S3Object s3Object : response.contents()) {

            if (s3Object.key().contains("-aws-billing-csv-")) {
                try {
                    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket("innogrid.cost").key(s3Object.key()).build();
                    ResponseInputStream<GetObjectResponse> or = s3.getObject(getObjectRequest);
                    FileOutputStream fos = new FileOutputStream(new File(s3Object.key()));
                    byte[] read_buf = new byte[1024];
                    int read_len = 0;
                    while ((read_len = or.read(read_buf)) > 0) {
                        fos.write(read_buf, 0, read_len);
                    }
                    or.close();
                    fos.close();
                } catch (FileNotFoundException e) {
                    logger.error("Failed to read AWS S3 csv file : '{}'", e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    logger.error("Failed to read AWS S3 csv file : '{}'", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        s3.close();

        return list;
    }

    public ResourceInfo getResourceUsage(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        IamClient iam = getIamClient(credentialInfo);
        RdsClient rds = getRdsClient(credentialInfo);
        S3Client s3 = getS3Client(credentialInfo);
        ElasticLoadBalancingV2Client elb = getElbClient(credentialInfo);

        ResourceInfo resourceInfo = new ResourceInfo();

        DescribeInstancesRequest ec2Request = DescribeInstancesRequest.builder().build();

        List<ServerInfo> list = new ArrayList<>();
        int running = 0;
        int stopped = 0;
        int etc = 0;
        Boolean done = false;

        while (!done) {
            DescribeInstancesResponse response = ec2.describeInstances(ec2Request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    ServerInfo info = new ServerInfo(instance);
                    list.add(info);
                    if (info.getState().equals("running")) {
                        running += 1;
                    } else if (info.getState().equals("stopped")) {
                        stopped += 1;
                    } else {
                        etc += 1;
                    }
                }
            }

            if (response.nextToken() == null) {
                done = true;
            }
        }

        DescribeVolumesRequest volumeRequest = DescribeVolumesRequest.builder().build();
        DescribeVolumesResponse volumeResponse = ec2.describeVolumes(volumeRequest);

        int diskUsage = 0;

        for (Volume volume : volumeResponse.volumes()) {
            VolumeInfo info = new VolumeInfo(volume);
            diskUsage += info.getSize();
        }

        DescribeSnapshotsRequest snapshotsRequest = DescribeSnapshotsRequest.builder().ownerIds("self").build();
        DescribeSnapshotsResponse snapshotsResponse = ec2.describeSnapshots(snapshotsRequest);

        DescribeImagesRequest imagesRequest = DescribeImagesRequest.builder().owners("self").build();
        DescribeImagesResponse imagesResponse = ec2.describeImages(imagesRequest);

        DescribeKeyPairsRequest keyPairsRequest = DescribeKeyPairsRequest.builder().build();
        DescribeKeyPairsResponse keyPairsResponse = ec2.describeKeyPairs(keyPairsRequest);

        DescribeSubnetsRequest subnetsRequest = DescribeSubnetsRequest.builder().build();
        DescribeSubnetsResponse subnetsResponse = ec2.describeSubnets(subnetsRequest);

        DescribeNetworkInterfacesRequest networkRequest = DescribeNetworkInterfacesRequest.builder().build();
        DescribeNetworkInterfacesResponse networkResponse = ec2.describeNetworkInterfaces(networkRequest);

        DescribeSecurityGroupsRequest sgRequest = DescribeSecurityGroupsRequest.builder().build();
        DescribeSecurityGroupsResponse sgResponse = ec2.describeSecurityGroups(sgRequest);

        DescribeAddressesRequest ipRequest = DescribeAddressesRequest.builder().build();
        DescribeAddressesResponse ipResponse = ec2.describeAddresses(ipRequest);

        ListUsersRequest userRequest = ListUsersRequest.builder().build();
        ListUsersResponse userResponse = iam.listUsers(userRequest);

        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3.listBuckets(listBucketsRequest);

        DescribeDbInstancesRequest describeDbInstancesRequest = DescribeDbInstancesRequest.builder().build();
        DescribeDbInstancesResponse describeDbInstancesResponse = rds.describeDBInstances(describeDbInstancesRequest);
        int databaseUsage = 0;

        for (DBInstance dbInstance : describeDbInstancesResponse.dbInstances()) {
            databaseUsage += dbInstance.allocatedStorage();
        }

        DescribeLoadBalancersRequest loadBalancersRequest = DescribeLoadBalancersRequest.builder().build();
        DescribeLoadBalancersResponse loadBalancersResponse = elb.describeLoadBalancers(loadBalancersRequest);

        iam.close();
        ec2.close();
        rds.close();
        s3.close();

        resourceInfo.setRunning(running);
        resourceInfo.setStop(stopped);
        resourceInfo.setEtc(etc);
        resourceInfo.setUsers(userResponse.users().size());
        resourceInfo.setNetworks(networkResponse.networkInterfaces().size());
        resourceInfo.setSecurityGroups(sgResponse.securityGroups().size());
        resourceInfo.setPublicIps(ipResponse.addresses().size());
        resourceInfo.setVolumes(volumeResponse.volumes().size());
        resourceInfo.setSnapshots(snapshotsResponse.snapshots().size());
        resourceInfo.setImages(imagesResponse.images().size());
        resourceInfo.setKeyPairs(keyPairsResponse.keyPairs().size());
        resourceInfo.setSubnets(subnetsResponse.subnets().size());
        resourceInfo.setDiskUsage(diskUsage);
        resourceInfo.setDatabaseCount(describeDbInstancesResponse.dbInstances().size());
        resourceInfo.setDatabaseUsage(databaseUsage);
        resourceInfo.setStorageCount(listBucketsResponse.buckets().size());
        resourceInfo.setLoadBalancer(loadBalancersResponse.loadBalancers().size());

        return resourceInfo;
    }

    public Image getImageDetail(CredentialInfo credentialInfo, String imageId) {
        if (credentialInfo == null) throw new CredentialException();

        Image imageInfo = null;
        try {
            Ec2Client ec2 = getEc2Client(credentialInfo);

            Filter filter = Filter.builder().name("image-id").values(imageId).build();
            DescribeImagesRequest request = DescribeImagesRequest.builder().filters(filter).build();

            DescribeImagesResponse response = ec2.describeImages(request);
            List<Image> list = response.images();

            imageInfo = list.size() > 0 ? list.get(0) : null;
        } catch (Exception e) {
            System.out.println(e);
        }
        return imageInfo;
    }

    public List<FlavorInfo> getFlavors(CredentialInfo credentialInfo, String osType) {
        if (credentialInfo == null) throw new CredentialException();

        AWSCredentials credentials = new BasicAWSCredentials(credentialInfo.getAccessId(), credentialInfo.getAccessToken());
        AWSPricing pricing = AWSPricingClientBuilder.standard()
                .withRegion(Regions.AP_SOUTH_1)
                .withClientConfiguration(new ClientConfiguration())
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        String location = "";
        for (Regions regions : Regions.values()) {
            if (regions.getName().equals(credentialInfo.getRegion())) {
                location = regions.getDescription();
            }
        }

        boolean isDone = false;
        String nextToken = null;

        List<String> resultList = new ArrayList<>();

        while (!isDone) {
            GetProductsResult result;

            if (nextToken == null) {
                GetProductsRequest request = new GetProductsRequest()
                        .withServiceCode("AmazonEC2")
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("location").withValue(location))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("operatingSystem").withValue(osType))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("currentGeneration").withValue("Yes"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("preInstalledSw").withValue("NA"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("licenseModel").withValue("No License required"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("capacitystatus").withValue("UnusedCapacityReservation"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("tenancy").withValue("Shared"));
                result = pricing.getProducts(request);
            } else {
                GetProductsRequest request = new GetProductsRequest()
                        .withServiceCode("AmazonEC2")
                        .withNextToken(nextToken)
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("location").withValue(location))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("operatingSystem").withValue(osType))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("currentGeneration").withValue("Yes"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("preInstalledSw").withValue("NA"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("licenseModel").withValue("No License required"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("capacitystatus").withValue("UnusedCapacityReservation"))
                        .withFilters(new com.amazonaws.services.pricing.model.Filter().withType(FilterType.TERM_MATCH).withField("tenant").withValue("Shared"));
                result = pricing.getProducts(request);
            }

            resultList.addAll(result.getPriceList());

            if (result.getNextToken() == null) {
                isDone = true;
            } else {
                nextToken = result.getNextToken();
            }
        }

        List<FlavorInfo> instanceTypeList = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < resultList.size(); i++) {
                FlavorInfo info = mapper.readValue(resultList.get(i), FlavorInfo.class);
                info.setInstanceTypeInfo();
                instanceTypeList.add(info);
            }
        } catch (IOException e) {
            logger.error("Failed to get flavor : '{}'", e.getMessage());
        }

        Collections.sort(instanceTypeList, Comparator.comparing(FlavorInfo::getInstanceType));

        return instanceTypeList;
    }

    public List<NetworkInfo> getNetwork(CredentialInfo credentialInfo, String NetworkId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Ec2Client ec2 = getEc2Client(credentialInfo);
        DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder().networkInterfaceIds(NetworkId).build();
        DescribeNetworkInterfacesResponse response = ec2.describeNetworkInterfaces(request);
        NetworkInfo info = new NetworkInfo(response.networkInterfaces().get(0));
        NetworkInfo2 info2 = new NetworkInfo2(response.networkInterfaces().get(0));
        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if (webCheck) {
            for (int i = 0; i < info.getId().length(); i++) {
                if (info.getId() != null && info.getId().equals(NetworkId)) {
                    list.add(info);
                    ec2.close();
                    break;
                } else {
                }
            }

        } else {
            for (int i = 0; i < info.getId().length(); i++) {

                if (info2.getId() != null && info.getId().equals(NetworkId)) {
                    list2.add(info2);
                    ec2.close();
                    break;
                } else {
                }
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        } else {
            return jsonArray2;
        }
    }


    public Object deleteNetwork(CredentialInfo credentialInfo, String NetworkId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        DeleteInfo deleteInfo = new DeleteInfo();
        List<NetworkInfo> list = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if (webCheck) {
            DescribeNetworkInterfacesRequest req = DescribeNetworkInterfacesRequest.builder().networkInterfaceIds(NetworkId).build();
            DescribeNetworkInterfacesResponse res = ec2.describeNetworkInterfaces(req);
            NetworkInfo info = new NetworkInfo(res.networkInterfaces().get(0));

            DeleteNetworkInterfaceRequest request = DeleteNetworkInterfaceRequest.builder().networkInterfaceId(NetworkId).build();
            DeleteNetworkInterfaceResponse response = ec2.deleteNetworkInterface(request);

            list.add(info);

            ec2.close();

            return list;
        }else{
            DescribeNetworkInterfacesRequest req = DescribeNetworkInterfacesRequest.builder().networkInterfaceIds(NetworkId).build();
            DescribeNetworkInterfacesResponse res = ec2.describeNetworkInterfaces(req);

            deleteInfo.setId(res.networkInterfaces().get(0).networkInterfaceId());

            DeleteNetworkInterfaceRequest request = DeleteNetworkInterfaceRequest.builder().networkInterfaceId(NetworkId).build();
            DeleteNetworkInterfaceResponse response = ec2.deleteNetworkInterface(request);

            ec2.close();
        }
        try {
            jsonString = mapper.writeValueAsString(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }
    }

    @Override
    public List<CredentialInfo> getCredential(List<CredentialInfo> list, String type) {
        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        CredentialInfo credentialInfo = new CredentialInfo();
        List<CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo info = list.get(i);
            if (info.getType().equals(type)) {
                credentialInfo.setId(list.get(i).getType());
                credentialInfo.setName(list.get(i).getName());
                credentialInfo.setType(list.get(i).getType() == "aws" ? "1" : "1");
                credentialInfo.setRegion(list.get(i).getRegion());
                credentialInfo.setAccessId(list.get(i).getAccessId());
                credentialInfo.setAccessToken(list.get(i).getAccessToken());
                credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
                credentialInfo.setCloudType(list.get(i).getCloudType());

                open.add(credentialInfo);
            }
        }

        try {
            jsonString = mapper.writeValueAsString(open);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        return jsonArray;
    }

    @Override
    public void deleteCredential(CredentialInfo credentialInfo, String projectId, String credentialId, CredentialDao credentialDao) {
        if (credentialInfo == null) throw new CredentialException();
        if (credentialInfo.getType().equals(credentialId)) {
            credentialDao.deleteCredential(credentialInfo);
        } else {
            throw new NullPointerException();
        }

    }

    @Override
    public Object createNetwork(CredentialInfo credentialInfo, Map<String, Object> data, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;

        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();

        Ec2Client ec2 = getEc2Client(credentialInfo);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        CreateNetworkInterfaceRequest request;

        if (webCheck) {
            String description = data.get("description").toString();
            String security = data.get("security").toString();
            String privateip = data.get("privateip").toString();
            String subnetid = data.get("subnetid").toString();

            if (privateip != "") {
                request = CreateNetworkInterfaceRequest.builder()
                        .description(description)
                        .groups(security)
                        .privateIpAddress(privateip)
                        .subnetId(subnetid)
                        .build();
            } else {
                request = CreateNetworkInterfaceRequest.builder()
                        .description(description)
                        .groups(security)
                        .privateIpAddress(null)
                        .subnetId(subnetid)
                        .build();
            }

            CreateNetworkInterfaceResponse response = ec2.createNetworkInterface(request);
            NetworkInfo info = new NetworkInfo(response.networkInterface());
            list.add(info);

            return list;
        }else{
            String name = data.get("name").toString();
            String ip = data.get("ip").toString();
            String subnet = data.get("subnetName").toString();
            String securityGroup = data.get("resourceGroupName").toString();

            if (ip != "") {
                request = CreateNetworkInterfaceRequest.builder()
                        .description(name)
                        .groups(securityGroup)
                        .privateIpAddress(ip)
                        .subnetId(subnet)
                        .build();
            } else {
                request = CreateNetworkInterfaceRequest.builder()
                        .description(name)
                        .groups(securityGroup)
                        .privateIpAddress(null)
                        .subnetId(subnet)
                        .build();
            }

            CreateNetworkInterfaceResponse response = ec2.createNetworkInterface(request);
            NetworkInfo2 info = new NetworkInfo2(response.networkInterface());
            list2.add(info);
        }try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        }else{
            return null;
        }
    }
}
