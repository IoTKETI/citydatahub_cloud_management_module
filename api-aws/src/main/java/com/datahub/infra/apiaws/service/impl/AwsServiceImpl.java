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
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.ImageService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;


import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;


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

                    list.add(info);
                    ec2.close();
                    break;
                } else {
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

                    ec2.close();
                    break;

                } else {

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

                    list.add(info);
                    ec2.close();
                    break;
                } else {

                }
            }
        }
        else{
            for (int i = 0; i < info2.getId().length(); i++) {

                if (info2.getId() != null && info2.getId().equals(id)) {

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
        }
        else {
            return jsonArray2;
        }
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
}
