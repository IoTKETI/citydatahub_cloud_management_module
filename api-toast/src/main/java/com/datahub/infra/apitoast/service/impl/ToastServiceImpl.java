package com.datahub.infra.apitoast.service.impl;

import com.datahub.infra.apitoast.service.ToastService;
import com.datahub.infra.coretoast.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;

import net.sf.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;


import com.datahub.infra.core.util.AES256Util;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief Toast API Service
 */
@Service
public class ToastServiceImpl extends RestApiAbstract implements ToastService {
    private final static Logger logger = LoggerFactory.getLogger(ToastServiceImpl.class);

    @Autowired
    private AES256Util aes256Util;

    private Timestamp tokenExpires;
    private String useToken;

     @Override
     public boolean validateCredential(CredentialInfo credentialInfo) {
         boolean isValid = true;
         if (credentialInfo == null) throw new CredentialException();

         JSONObject jsonObj = new JSONObject();
         JSONObject finalJsonObject = new JSONObject();
         JSONObject postData = new JSONObject();
         jsonObj.put("username", credentialInfo.getAccessId());
         jsonObj.put("password", credentialInfo.getAccessToken());
         finalJsonObject.put("passwordCredentials", jsonObj);
         finalJsonObject.put("tenantId", credentialInfo.getTenant());
         postData.put("auth", finalJsonObject);

         try{
             HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl( KR_IDENTITY_DOMAIN + "tokens"), new JsonHttpContent(JSON_FACTORY, postData));
             httpRequest.execute().parseAs(TokensInfo.class);
         }catch(HttpResponseException httpResponseExceptin){
             isValid = false;
         }catch(IOException ioException){
             isValid = false;
             ioException.printStackTrace();
         }catch(Exception exception){
             isValid = false;
             exception.printStackTrace();
         }
         return isValid;
     }

    @Override
     public List<Servers> getServers(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();
         getToken(credentialInfo);

        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

         try{
             HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers/detail"));
             ServersInfo serversInfo = getRequest.execute().parseAs(ServersInfo.class);

             if(serversInfo.getServers() != null){
                 jsonString = mapper.writeValueAsString(serversInfo.getServers());
             }
         }catch(IOException ioException){
             ioException.printStackTrace();
         }catch(Exception exception){
             exception.printStackTrace();
         }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        return jsonArray;
    }


    @Override
    public ServerInfo getServer(CredentialInfo credentialInfo, String serverId) {

        ServerInfo serverInfo = new ServerInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers/" + serverId));
            serverInfo = getRequest.execute().parseAs(ServerInfo.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return serverInfo;
    }

    @Override
     public List<Volumes> getVolumes(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();
        getToken(credentialInfo);

        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);


         try{
             HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/volumes/detail"));
             VolumesInfo volumesInfo = getRequest.execute().parseAs(VolumesInfo.class);

             if(volumesInfo.getVolumes() != null){
                 jsonString = mapper.writeValueAsString(volumesInfo.getVolumes());
             }

         }catch(IOException e){
             e.printStackTrace();
         }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
         return jsonArray;
     }


    @Override
    public VolumeInfo getVolume(CredentialInfo credentialInfo, String volumeId) {

        VolumeInfo volumeInfo = new VolumeInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/volumes/" + volumeId));
            volumeInfo = getRequest.execute().parseAs(VolumeInfo.class);
        }catch(IOException e){
            e.printStackTrace();
        }
        return volumeInfo;
    }

    @Override
     public List<Snapshots> getSnapshots(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();

        List<Snapshots> infoList = null;

         try{
             HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/snapshots/detail"));
             SnapshotsInfo snapshotsInfo = getRequest.execute().parseAs(SnapshotsInfo.class);

             if(snapshotsInfo.getSnapshots() != null){
                 infoList = Arrays.asList(snapshotsInfo.getSnapshots());
             }
         }catch(IOException ioException){
             ioException.printStackTrace();
         }catch(Exception exception){
             exception.printStackTrace();
         }
        JSONArray jsonArray = JSONArray.fromObject(infoList);
        return jsonArray;
    }


    @Override
    public SnapshotInfo getSnapshot(CredentialInfo credentialInfo, String snapshotId) {

        SnapshotInfo info = new SnapshotInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/snapshots/" + snapshotId));
            info = getRequest.execute().parseAs(SnapshotInfo.class);
        }catch(IOException e){
            e.printStackTrace();
        }
        return info;
    }

    @Override
    public ResourceInfo getResourceUsage(CredentialInfo credentialInfo){
        if (credentialInfo == null) throw new CredentialException();
        getToken(credentialInfo);

        List<Servers> servers = null;
        List<Volumes> volumes = null;
        List<Networks> networks = null;
        List<Snapshots> snapshots = null;
        List<SecurityGroups> securityGroups = null;
        List<Floatingips> floatingIps = null;

        try{
            HttpRequest getRequest_server = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers/detail"));
            ServersInfo serversInfo = getRequest_server.execute().parseAs(ServersInfo.class);

            HttpRequest getRequest_volumes = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/volumes/detail"));
            VolumesInfo volumesInfo = getRequest_volumes.execute().parseAs(VolumesInfo.class);

            HttpRequest getRequest_networks = jsonRequestFactory.buildGetRequest(new GenericUrl("https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/networks"));
            NetworkInfo networksInfo = getRequest_networks.execute().parseAs(NetworkInfo.class);

            HttpRequest getRequest_snapshots = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/snapshots/detail"));
            SnapshotsInfo snapshotsInfo = getRequest_snapshots.execute().parseAs(SnapshotsInfo.class);

            HttpRequest getRequest_securityGroups = jsonRequestFactory.buildGetRequest(new GenericUrl("https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/security-groups"));
            SecurityGroupsInfo securityGroupsInfo = getRequest_securityGroups.execute().parseAs(SecurityGroupsInfo.class);

            HttpRequest getRequest_floatingIps = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_NETWORK_DOMAIN  + "/floatingips"));
            FloatingipsInfo floatingIpsInfo = getRequest_floatingIps.execute().parseAs(FloatingipsInfo.class);


            if(serversInfo.getServers() != null){
                servers = Arrays.asList(serversInfo.getServers());
            }
            if(volumesInfo.getVolumes() != null) {
                volumes = Arrays.asList(volumesInfo.getVolumes());
            }
            if(networksInfo.getNetworks() != null){
                networks = Arrays.asList(networksInfo.getNetworks());
            }
            if(snapshotsInfo.getSnapshots() != null){
                snapshots = Arrays.asList(snapshotsInfo.getSnapshots());
            }
            if(securityGroupsInfo.getSecurity_groups() != null){
                securityGroups = Arrays.asList(securityGroupsInfo.getSecurity_groups());
            }
            if(floatingIpsInfo.getFloatingips() != null){
                floatingIps = Arrays.asList(floatingIpsInfo.getFloatingips());
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        ResourceInfo resourceInfo = new ResourceInfo();

        int running = 0;
        int stopped = 0;
        int etc = 0;
        int total = 0;
        int volumesUsage = 0;


        for(Servers serverInfo : servers){
            if(serverInfo.getStatus().equals("ACTIVE")){
                running += 1;
            }else if(serverInfo.getStatus().equals("SHUTOFF")){
                stopped += 1;
            }else{
                etc += 1;
            }
        }
        total = running + stopped + etc;

        for(Volumes volumesInfo : volumes){
            volumesUsage += volumesInfo.getSize();
        }

        if(credentialInfo != null){
            resourceInfo.setUsers(1);
        }
        resourceInfo.setRunning(running);
        resourceInfo.setStop(stopped);
        resourceInfo.setEtc(etc);
        resourceInfo.setTotal(total);
        resourceInfo.setVolumeUsage(volumesUsage);
        resourceInfo.setVolumes(volumes.size());
        resourceInfo.setSnapshots(snapshots.size());
        resourceInfo.setNetworks(networks.size());
        resourceInfo.setSecurityGroups(securityGroups.size());
        resourceInfo.setPublicIp(floatingIps.size());
        return resourceInfo;
    }

    @Override
    public List<CredentialInfo> getCredential(List<CredentialInfo> list, String type) {
        List<CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo info = list.get(i);
            if (info.getType().equals(type)) {
                open.add(list.get(i));
            }
        }
        return open;
    }
    @Override
    public void deleteCredential(CredentialInfo credentialInfo, String projectId, String credentialId, CredentialDao credentialDao) {
        if (credentialInfo == null) throw new CredentialException();
        if (credentialInfo.getId().equals(credentialId)){
            credentialDao.deleteCredential(credentialInfo);
        }else{
            throw new NullPointerException();
        }

    }

    @Override
    public VolumeInfo createVolume(CredentialInfo credentialInfo, Map<String, Object> createData) {
        if (credentialInfo == null) throw new CredentialException();

         String volumeName = createData.get("name").toString();
         String volumeDescription = createData.get("description").toString();
         String volumeSize = createData.get("size").toString();
         String volumeType = createData.get("volume_type").toString();
         String volumeAvailability_zone = createData.get("availability_zone").toString();

        if (volumeType == "0") {
            volumeType = "General HDD";
        } else {
            volumeType = "General SSD";
        }

         JSONObject finalJsonObject = new JSONObject();
         JSONObject postData = new JSONObject();
         finalJsonObject.put("size", volumeSize);
         finalJsonObject.put("availability_zone", volumeAvailability_zone);
         finalJsonObject.put("description", volumeDescription);
         finalJsonObject.put("name", volumeName);
         finalJsonObject.put("volume_type", volumeType);
         postData.put("volume", finalJsonObject);

         VolumeInfo info = new VolumeInfo();

         try{
             HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl( KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/volumes"), new JsonHttpContent(JSON_FACTORY, postData));
             info = httpRequest.execute().parseAs(VolumeInfo.class);
             return info;

         }catch(HttpResponseException httpResponseExceptin){
             logger.error("Http Response Exception : [{}]", httpResponseExceptin.getMessage());
         }catch(IOException ioException){
             ioException.printStackTrace();
         }catch(Exception exception){
             exception.printStackTrace();
         }

         return null;
     }

     @Override
     public Header deleteServer(CredentialInfo credentialInfo, String serverId) {
         if (credentialInfo == null) throw new CredentialException();

         Header info = new Header();

         try{
             HttpRequest getRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers/" + serverId));
             info = getRequest.execute().parseAs(Header.class);
         }catch(IOException e){
            e.printStackTrace();
        }
         return info;
     }

     @Override
     public Header deleteVolume(CredentialInfo credentialInfo, String volumeId) {
         if (credentialInfo == null) throw new CredentialException();

         Header info = new Header();

         try{
             HttpRequest getRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/volumes/" + volumeId));
         }catch(HttpResponseException httpResponseExceptin){
             logger.error("Http Response Exception : [{}]", httpResponseExceptin.getMessage());
         }catch(IOException e){
            e.printStackTrace();
        }
         return info;
     }

     @Override
     public Header deleteSnapshot(CredentialInfo credentialInfo, String snapshotId) {
         if (credentialInfo == null) throw new CredentialException();

         Header info = new Header();

         try{
             HttpRequest getRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/snapshots/" + snapshotId));
             info = getRequest.execute().parseAs(Header.class);
         }catch(IOException e){
            e.printStackTrace();
        }
         return info;
     }

     @Override
     public TypeInfo getVolumeTypes(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();

         TypeInfo typeInfo = new TypeInfo();

         try{
             HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_BLOCK_DOMAIN + credentialInfo.getTenant() + "/types"));
             typeInfo = getRequest.execute().parseAs(TypeInfo.class);
         }catch(IOException e){
             e.printStackTrace();
         }
         return typeInfo;
     }

    @Override
    public List<Networks> getNetworks(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();

        getToken(credentialInfo);

        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try{

            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl( KR_NETWORK_DOMAIN  + "/networks"));
            NetworkInfo networkInfo = getRequest.execute().parseAs(NetworkInfo.class);

            if(networkInfo.getNetworks() != null){
                jsonString = mapper.writeValueAsString(networkInfo.getNetworks());
            }

        }catch(IOException e){
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);

        if(jsonArray.size() > 1) {
            jsonArray.remove(0);
        }
        return jsonArray;
    }


    @Override
    public List<Subnets> getSubnets(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();

        List<Subnets> subnetInfo = new ArrayList<>();
        List<Subnets> subnetTemp = null;

        try {

            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl("https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/subnets"));
            SubnetInfo listInfo = getRequest.execute().parseAs(SubnetInfo.class);

            if(listInfo.getSubnets() != null){
                subnetTemp = Arrays.asList(listInfo.getSubnets());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        for(Subnets temp : subnetTemp){
            if(temp.getEnable_dhcp()){
                logger.error("For : {}", temp);
                subnetInfo.add(temp);
                logger.error("For : {}", subnetInfo);
            }
        }
        return subnetInfo;
    }

    @Override
    public List<SecurityGroups> getSecurityGroups(CredentialInfo credentialInfo) {
         if (credentialInfo == null) throw new CredentialException();
        getToken(credentialInfo);
        List<SecurityGroups> securityGroupsInfo = null;

        try{

            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl( KR_NETWORK_DOMAIN  + "/security-groups"));
            SecurityGroupsInfo listInfo = getRequest.execute().parseAs(SecurityGroupsInfo.class);

            if(listInfo.getSecurity_groups() != null){
                securityGroupsInfo = Arrays.asList(listInfo.getSecurity_groups());
            }

        }catch(IOException e){
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(securityGroupsInfo);
        return jsonArray;
    }
    @Override
    public SecurityGroupInfo getSecurityGroups(CredentialInfo credentialInfo, String securityId) {
         if (credentialInfo == null) throw new CredentialException();

        SecurityGroupInfo securityGroupInfo = new SecurityGroupInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_NETWORK_DOMAIN  + "/security-groups/" + securityId));
            securityGroupInfo = getRequest.execute().parseAs(SecurityGroupInfo.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return securityGroupInfo;
    }

    @Override
    public Header deleteSecurityGroup(CredentialInfo credentialInfo, String securityId) {
         if (credentialInfo == null) throw new CredentialException();

        Header header = new Header();
        try{
            HttpRequest httpRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_NETWORK_DOMAIN  + "/security-groups/" + securityId));
            header = httpRequest.execute().parseAs(Header.class);
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        return header;
    }

    @Override
    public List<Floatingips> getFloatingips(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();
        getToken(credentialInfo);
        List<Floatingips> floatingipsInfo = null;

        try{

            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_NETWORK_DOMAIN  + "/floatingips"));
            FloatingipsInfo listInfo = getRequest.execute().parseAs(FloatingipsInfo.class);

            if(listInfo.getFloatingips() != null){
                floatingipsInfo = Arrays.asList(listInfo.getFloatingips());
            }

        }catch(IOException e){
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(floatingipsInfo);
        return jsonArray;
    }

    @Override
    public FloatingipInfo getFloatingip(CredentialInfo credentialInfo, String floatingIpId) {
         if (credentialInfo == null) throw new CredentialException();

        FloatingipInfo floatingipInfo = new FloatingipInfo();

        try{

            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl("https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/floatingips/"+floatingIpId));
            floatingipInfo = getRequest.execute().parseAs(FloatingipInfo.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return floatingipInfo;
    }

    @Override
    public Header deleteFloatingip(CredentialInfo credentialInfo, String floatingIpId) {
         if (credentialInfo == null) throw new CredentialException();

        Header header = new Header();
        try{
            HttpRequest httpRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl("https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/floatingips/" + floatingIpId));
            header = httpRequest.execute().parseAs(Header.class);
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        return header;
    }

    @Override
    public List<ImageDetailInfo> getImageDetails(CredentialInfo credentialInfo, String type, String location) {

        List<ImageDetailInfo> imageInfoList = null;

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildGetRequest(new GenericUrl("https://kr1-api-image.infrastructure.cloud.toast.com/v2/images"));
            ImageInfoApi imageInfoApi = httpRequest.execute().parseAs(ImageInfoApi.class);

            if(imageInfoApi.getImages() != null){
                imageInfoList = Arrays.asList(imageInfoApi.getImages());
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(imageInfoList);

        return jsonArray;
    }

    @Override
    public TokensInfo getToken(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Timestamp currentTime = new Timestamp(new Date().getTime());
        Timestamp expireTime;

        JSONObject jsonObj = new JSONObject();
        JSONObject finalJsonObject = new JSONObject();
        JSONObject postData = new JSONObject();
        jsonObj.put("username", credentialInfo.getAccessId());
        jsonObj.put("password", credentialInfo.getAccessToken());
        finalJsonObject.put("passwordCredentials", jsonObj);
        finalJsonObject.put("tenantId", credentialInfo.getTenant());
        postData.put("auth", finalJsonObject);

        TokensInfo info = new TokensInfo();

        if(tokenExpires == null || currentTime.after(tokenExpires)) {
            if(tokenExpires == null) { logger.error("[토큰이 발급된 적 없음]"); }
            if(tokenExpires != null && currentTime.after(tokenExpires)) { logger.error("[토큰 시간 만료 확인으로 인한 재발급 절차 진행]"); }

            try{
                HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl( KR_IDENTITY_DOMAIN + "tokens"), new JsonHttpContent(JSON_FACTORY, postData));
                info = httpRequest.execute().parseAs(TokensInfo.class);
                Date getExpires = dateFormat2.parse(info.getAccess().getToken().getExpires());
                expireTime = new Timestamp(getExpires.getTime());
                tokenExpires = expireTime;
                receiveToken(info.getAccess().getToken().getId());
                return info;

            }catch(HttpResponseException httpResponseExceptin){
                logger.error("Http Response Exception : [{}]", httpResponseExceptin.getMessage());
            }catch(IOException ioException){
                ioException.printStackTrace();
            }catch(Exception exception){
                exception.printStackTrace();
            }
        } else if(tokenExpires.after(currentTime)) {
            logger.error("토큰 시간 아직 남음");
        }
        return null;
    }


    @Override
    public ServerInfo createServers(CredentialInfo credentialInfo, JSONObject createData) {
        if (credentialInfo == null) throw new CredentialException();
        ServerInfo info = new ServerInfo();

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers"), new JsonHttpContent(JSON_FACTORY, createData));
            info = httpRequest.execute().parseAs(ServerInfo.class);
        }
        catch(HttpResponseException httpResponseExceptin) {
            logger.error("Http Response Exception : [{}]", httpResponseExceptin.getMessage());
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        return info;
    }

    @Override
    public String serverAction(CredentialInfo credentialInfo, JSONObject actionData, String instanceId){

        if (credentialInfo == null) throw new CredentialException();
        String info = null;
        Runnable function = null;

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/servers/" + instanceId + "/action"), new JsonHttpContent(JSON_FACTORY, actionData));
            info = httpRequest.execute().parseAsString();
        }catch(HttpResponseException httpResponseExceptin){
            logger.error("Http Response Exception : [{}]", httpResponseExceptin.getMessage());
            info =  httpResponseExceptin.getMessage();
            return info;
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }

        if(function != null) {
            new Thread(function).start();
        }
        return info;
    }


    @Override
    public List<ImageDetailInfo> getImageDetails(CredentialInfo credentialInfo) {

        List<ImageDetailInfo> imageInfoList = null;
        try{
            HttpRequest httpRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_IMAGE_DOMAIN + "/images"));
            ImageInfoApi imageInfoApi = httpRequest.execute().parseAs(ImageInfoApi.class);

            if(imageInfoApi.getImages() != null){
                imageInfoList = Arrays.asList(imageInfoApi.getImages());
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(imageInfoList);
        return jsonArray;
    }

    @Override
    public ImageDetailInfo getImagesDetails(CredentialInfo credentialInfo, String imageId) {

        ImageDetailInfo info = new ImageDetailInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_IMAGE_DOMAIN + "/images/" + imageId));
            info = getRequest.execute().parseAs(ImageDetailInfo.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return info;
    }

    @Override
    public Header deleteImage(CredentialInfo credentialInfo, String imageId) {
        if (credentialInfo == null) throw new CredentialException();

        Header info = new Header();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_IMAGE_DOMAIN + "/images/" + imageId));
            info = getRequest.execute().parseAs(Header.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return info;
    }

    @Override
    public List<ZoneDetailInfo> getZoneDetails(CredentialInfo credentialInfo) {

        List<ZoneDetailInfo> zoneInfoList = null;
        try{
            HttpRequest httpRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/os-availability-zone"));
            ZoneInfoApi zoneInfoApi = httpRequest.execute().parseAs(ZoneInfoApi.class);

            if(zoneInfoApi.getAvailabilityZoneInfo() != null){
                zoneInfoList = Arrays.asList(zoneInfoApi.getAvailabilityZoneInfo());
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(zoneInfoList);

        return jsonArray;
    }

    @Override
    public List<FlavorDetailInfo> getFlavorDetails(CredentialInfo credentialInfo) {

        List<FlavorDetailInfo> flavorInfoList = null;

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/flavors/detail"));
            FlavorInfoApi flavorInfoApi = httpRequest.execute().parseAs(FlavorInfoApi.class);

            if(flavorInfoApi.getFlavors() != null){
                flavorInfoList = Arrays.asList(flavorInfoApi.getFlavors());
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(flavorInfoList);

        return jsonArray;
    }

    @Override
    public List<Keypair> getKeypairDetails(CredentialInfo credentialInfo) {
        getToken(credentialInfo);
        List<Keypair> keypairInfoList = new ArrayList<>();

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/os-keypairs"));
            KeypairInfoApi keypairInfoApi = httpRequest.execute().parseAs(KeypairInfoApi.class);

            if(keypairInfoApi.getKeypairs() != null){
                List<KeypairDetailInfo> tempList = Arrays.asList(keypairInfoApi.getKeypairs());
                for(KeypairDetailInfo temp : tempList){
                    keypairInfoList.add(temp.getKeypair());
                }
                logger.error("tempList : {}", keypairInfoList);
            }
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(keypairInfoList);
        return jsonArray;
    }


    @Override
    public KeypairsDetailInfo postKeypair(CredentialInfo credentialInfo, String keypairData){

        if (credentialInfo == null) throw new CredentialException();
        KeypairsDetailInfo info = new KeypairsDetailInfo();
        Runnable function = null;

        JSONParser parser = new JSONParser();
        Object obj = null;
        try {
            obj = parser.parse( keypairData );
        } catch (ParseException e) {
            e.printStackTrace();
        }
        JSONObject jsonObj = (JSONObject) obj;

        try{
            HttpRequest httpRequest = jsonRequestFactory.buildPostRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/os-keypairs"), new JsonHttpContent(JSON_FACTORY, jsonObj));
            info = httpRequest.execute().parseAs(KeypairsDetailInfo.class);
        }catch(IOException ioException){
            ioException.printStackTrace();
        }catch(Exception exception){
            exception.printStackTrace();
        }

        if(function != null) {
            new Thread(function).start();
        }

        return info;
    }

    @Override
    public KeypairsDetailInfo getKeypairsDetails(CredentialInfo credentialInfo, String keypairName) {

        KeypairsDetailInfo info = new KeypairsDetailInfo();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildGetRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/os-keypairs/" + keypairName));
            info = getRequest.execute().parseAs(KeypairsDetailInfo.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return info;
    }

    @Override
    public Header deleteKeypair(CredentialInfo credentialInfo, String keypairName) {
        if (credentialInfo == null) throw new CredentialException();

        Header info = new Header();

        try{
            HttpRequest getRequest = jsonRequestFactory.buildDeleteRequest(new GenericUrl(KR_INSTANCE_DOMAIN + credentialInfo.getTenant() + "/os-keypairs/" + keypairName));
            info = getRequest.execute().parseAs(Header.class);

        }catch(IOException e){
            e.printStackTrace();
        }
        return info;
    }
}
