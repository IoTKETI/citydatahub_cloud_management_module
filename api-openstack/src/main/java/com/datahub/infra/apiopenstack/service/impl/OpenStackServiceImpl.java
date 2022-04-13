package com.datahub.infra.apiopenstack.service.impl;

import com.datahub.infra.apiopenstack.service.OpenStackService;
import com.datahub.infra.core.exception.CityHubUnAuthorizedException;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coreopenstack.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.network.*;
import org.openstack4j.model.network.builder.NetworkBuilder;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.openstack4j.openstack.OSFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenStackServiceImpl implements OpenStackService {
    private final static Logger logger = LoggerFactory.getLogger(OpenStackServiceImpl.class);

    private Map<String, List> projectMap = new HashMap<>();

    private OSClient getOpenstackClient(CredentialInfo info, String projectId) {
        OSFactory.enableHttpLoggingFilter(true);

        Identifier domainIdentifier = Identifier.byId(info.getDomain());
        OSClient os = null;
        try {
            if (info.getProjectId() == null) {
                os = OSFactory.builderV3()
                        .endpoint(info.getUrl())
                        .credentials(info.getAccessId(), info.getAccessToken(), domainIdentifier)
                        .authenticate();
            } else {
                if (projectId != null) {
                    os = OSFactory.builderV3()
                            .endpoint(info.getUrl())
                            .credentials(info.getAccessId(), info.getAccessToken(), domainIdentifier)
                            .scopeToProject(Identifier.byId(projectId))
                            .authenticate();
                } else {
                    os = OSFactory.builderV3()
                            .endpoint(info.getUrl())
                            .credentials(info.getAccessId(), info.getAccessToken(), domainIdentifier)
                            .scopeToProject(Identifier.byId(info.getProjectId()))
                            .authenticate();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get openstack credential : '{}'", e.getMessage());
            throw new CityHubUnAuthorizedException(e.getMessage());
        }

        return os;
    }

    @Override
    public boolean validateCredential(CredentialInfo credentialInfo) {
        boolean isValid = true;

        try {
            OSClient os = getOpenstackClient(credentialInfo, null);

            List<? extends Project> projects = ((OSClient.OSClientV3) os).identity().projects().list();

            if(projects == null) {
                isValid = false;
            }

        } catch (Exception e) {
            logger.error("Failed to validate credential : '{}'", e.getMessage());
            isValid = false;
        }
        return isValid;
    }

    @Override
    public List<ServerInfo> getServers(CredentialInfo credentialInfo, String projectId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        if(webCheck) {
            List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
                if(projectId == null) {
                    put("all_tenants", "true");
                }
            }});
            for (int j = 0; j < openstackServers.size(); j++) {
                Server server = openstackServers.get(j);
                ServerInfo info = new ServerInfo(server);
                if (info.getProjectId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
                }

                list.add(info);
            }
        }
        else{
            List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
                if(projectId == null) {
                    put("all_tenants", "true");
                }
            }});

            List<NetworkInfo> networkInfoList= getNetworks(credentialInfo,projectId,true);

            List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false, null);

            for (int j = 0; j < openstackServers.size(); j++) {
                Server server = openstackServers.get(j);

                ServerInfo2 info = new ServerInfo2(server, networkInfoList);

                for(ImageInfo temp : imageInfos){
                    if(temp.getId().equals(server.getImageId())){
                        info.setImageName(temp.getType());
                    }
                }

                list2.add(info);
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException ignored) {
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }

    @Override
    public List<ServerInfo> getServers_model(CredentialInfo credentialInfo, String projectId, Boolean webCheck, Object serverModel){
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        if(webCheck) {
            List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
                if(projectId == null) {
                    put("all_tenants", "true");
                }
            }});
            for (int j = 0; j < openstackServers.size(); j++) {
                Server server = openstackServers.get(j);
                ServerInfo info = new ServerInfo(server);
                if (info.getProjectId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
                }

                list.add(info);
            }
        }
        else{
            List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{

                if(projectId == null) {
                    put("all_tenants", "true");
                }

                if(serverModel != null){
                    String string_serverModel = serverModel.toString();
                    int lineCnt = 0;
                    int fromIndex = -1;
                    while ((fromIndex = string_serverModel.indexOf(":", fromIndex + 1)) >= 0) {
                        lineCnt++;
                    }

                    JSONArray jsonArray_model = JSONArray.fromObject(serverModel);

                    JSONObject jsonObj = jsonArray_model.getJSONObject(0);

                    Set key = jsonObj.keySet();

                    Iterator iter = key.iterator();

                    while(iter.hasNext()){
                        Object keyName = iter.next();

                        put((String) keyName, jsonObj.getString((String) keyName));
                    }
                }
            }});

            List<NetworkInfo> networkInfoList= getNetworks(credentialInfo,projectId,true);

            List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false, null);

            for (int j = 0; j < openstackServers.size(); j++) {
                Server server = openstackServers.get(j);

                ServerInfo2 info = new ServerInfo2(server, networkInfoList);

                for(ImageInfo temp : imageInfos){
                    if(temp.getId().equals(server.getImageId())){
                        info.setImageName(temp.getType());
                    }
                }

                list2.add(info);
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException ignored) {
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }

    @Override
    public List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String projectId, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
        }});

        List<ServerInfo2> list2 = new ArrayList<>();

        List<NetworkInfo> networkInfoList= getNetworks(credentialInfo,projectId,true);

        for (int j = 0; j < openstackServers.size(); j++) {
            Server server = openstackServers.get(j);
            ServerInfo2 info = new ServerInfo2(server, networkInfoList);

            List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false, null);
            for(ImageInfo temp : imageInfos){
                if(temp.getId().equals(server.getImageId())){
                    info.setImageName(temp.getType());
                }
            }

            if(type.equals("name")){
                if(info.getName().equals(value)) list2.add(info);
            }

            if(type.equals("serverState")){
                if(info.getState().equals(value)) list2.add(info);
            }
        }
        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        return jsonArray2;
    }

    @Override
    public List<ServerInfo> getServer(CredentialInfo credentialInfo, String projectId, String serverId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Server openstackServer = os.compute().servers().get(serverId);


        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        if(webCheck) {
            ServerInfo info = new ServerInfo(openstackServer);

            if (info.getId() == null || info.getId().equals("")) {
                info.setId(serverId);
            }

            if (info.getProjectId() != null) {
                info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
            }

            for (int i = 0; i < info.getId().length(); i++) {

                if (info.getId() != null && info.getId().equals(serverId)) {
                    list.add(info);
                    break;
                } else {

                }
            }
        } else {
            List<NetworkInfo> networkInfoList= getNetworks(credentialInfo, projectId, true);
            ServerInfo2 info = new ServerInfo2(openstackServer,networkInfoList);

            if (info.getId() == null || info.getId().equals("")) {
                info.setId(serverId);
            }

            for (int i = 0; i < info.getId().length(); i++) {

                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false, null);
                for(ImageInfo temp : imageInfos){
                    if(temp.getId().equals(info.getImageId())){
                        info.setImageName(temp.getType());
                    }
                }

                if (info.getId() != null && info.getId().equals(serverId)) {
                    list2.add(info);
                    break;
                } else {

                }
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }

    @Override
    public List<VolumeInfo> getVolumes(CredentialInfo credentialInfo, String projectId, Boolean webCheck) {
        return getVolumes(credentialInfo, projectId, false, false, webCheck);
    }

    @Override
    public List<VolumeInfo> getVolumes(CredentialInfo credentialInfo, String projectId, Boolean bootable, Boolean available, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Volume> openstackvolumes = os.blockStorage().volumes().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
            if(bootable) {
                put("bootable", "true");
            }
            if(available) {
                put("status", "available");
            }
        }});
        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();

        if(webCheck) {
            if (openstackvolumes.size() > 0) {
                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, webCheck, null);

                for (int j = 0; j < openstackvolumes.size(); j++) {
                    Volume volume = openstackvolumes.get(j);

                    VolumeInfo info = new VolumeInfo(volume);

                    list.add(info);
                }
                return list;
            }
        } else {
            if (openstackvolumes.size() > 0) {
                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false, null);
                for (int j = 0; j < openstackvolumes.size(); j++) {
                    Volume volume = openstackvolumes.get(j);

                    VolumeInfo2 info = new VolumeInfo2(volume);
                    list2.add(info);
                }
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }

    @Override
    public List<VolumeInfo> getVolumes_Search(CredentialInfo credentialInfo, String projectId, Boolean bootable, Boolean available, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Volume> openstackvolumes = os.blockStorage().volumes().list(new HashMap<String, String>(){{
            if(projectId == null) put("all_tenants", "true");
            if(bootable) put("bootable", "true");
            if(available) put("status", "available");
        }});

        List<VolumeInfo2> list2 = new ArrayList<>();

        if (openstackvolumes.size() > 0) {
            for (int j = 0; j < openstackvolumes.size(); j++) {
                Volume volume = openstackvolumes.get(j);

                VolumeInfo2 info = new VolumeInfo2(volume);

                if(type.equals("name")){
                    if(info.getName().equals(value)) list2.add(info);
                }
                if(type.equals("volumeState")){
                    if(info.getState().equals(value)) list2.add(info);
                }
            }
        }
        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }

    @Override
    public List<VolumeInfo> getVolume(CredentialInfo credentialInfo, String projectId, String volumeId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Volume openstackVolume = os.blockStorage().volumes().get(volumeId);

        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();


        if(webCheck) {
            VolumeInfo info = new VolumeInfo(openstackVolume);

            if (openstackVolume != null) {
                List<ServerInfo> servers = getServers(credentialInfo, projectId, webCheck);

                if (info.getProjectId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
                }
                info.setServerNameForVolumeAttachmentInfos(servers);
            }
            for (int i = 0; i < info.getId().length(); i++) {
                if (info.getId() != null && info.getId().equals(volumeId)) {
                    list.add(info);
                    break;
                } else {
                }
            }
        } else {
            VolumeInfo2 info = new VolumeInfo2(openstackVolume);
            for (int i = 0; i < info.getId().length(); i++) {
                if (info.getId() != null && info.getId().equals(volumeId)) {
                    list2.add(info);
                    break;
                } else {
                }
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }


    @Override
    public Object createVolume(CredentialInfo credentialInfo, String projectId, CreateVolumeInfo createVolumeInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();
        OSClient os = getOpenstackClient(credentialInfo, projectId);
        String jsonString = null;
        String jsonString2 = null;
        List<VolumeInfo> list = new ArrayList<>();
        List<VolumeInfo2> list2 = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        if(webCheck) {
            VolumeBuilder builder = Builders.volume()
                    .name(createVolumeInfo.getName())
                    .description(createVolumeInfo.getDescription())
                    .size(createVolumeInfo.getSize())
                    .zone(createVolumeInfo.getAvailabilityZone());
            if (createVolumeInfo.getVolumeType() != null && !createVolumeInfo.getVolumeType().equals("")) {
                builder.volumeType(createVolumeInfo.getVolumeType());
            }

            if (createVolumeInfo.getSourceType().equals("image")) {
                builder.imageRef(createVolumeInfo.getSourceId());
            } else if (createVolumeInfo.getSourceType().equals("volume")) {
                builder.source_volid(createVolumeInfo.getSourceId());
            } else if (createVolumeInfo.getSourceType().equals("snapshot")) {
                builder.snapshot(createVolumeInfo.getSourceId());
            }

            if (createVolumeInfo.getSourceType() != null && !createVolumeInfo.getSourceType().equals("empty") && !createVolumeInfo.getSourceType().equals("")) {
                if (createVolumeInfo.getSourceSize().intValue() < createVolumeInfo.getSize().intValue()) {
                    builder.size(createVolumeInfo.getSourceSize());
                }
                builder.size(createVolumeInfo.getSourceSize());
            }
            Volume volume = os.blockStorage().volumes().create(builder.build());
            list=getVolume(credentialInfo, projectId, volume.getId(), true);
        }
        else{
            VolumeBuilder builder = Builders.volume()
                    .name(createVolumeInfo.getName())
                    .description(createVolumeInfo.getDescription())
                    .size(createVolumeInfo.getSize())
                    .zone(createVolumeInfo.getAvailabilityZone());

            if (createVolumeInfo.getVolumeType() != null) {
                if(!createVolumeInfo.getVolumeType().equals("")){
                    builder.volumeType(createVolumeInfo.getVolumeType());
                }
            }else{
                throw new NullPointerException("RequestBody Null Attribute.");
            }
            if(createVolumeInfo.getSourceType() != null){
                if (createVolumeInfo.getSourceType().equals("image")) {
                    builder.imageRef(createVolumeInfo.getSourceId());
                } else if (createVolumeInfo.getSourceType().equals("volume")) {
                    builder.source_volid(createVolumeInfo.getSourceId());
                } else if (createVolumeInfo.getSourceType().equals("snapshot")) {
                    builder.snapshot(createVolumeInfo.getSourceId());
                }
            }

            if (createVolumeInfo.getSourceType() != null && !createVolumeInfo.getSourceType().equals("empty") && !createVolumeInfo.getSourceType().equals("")) {
                if (createVolumeInfo.getSourceSize().intValue() < createVolumeInfo.getSize().intValue()) {
                    builder.size(createVolumeInfo.getSourceSize());
                }
            }


            Volume volume = os.blockStorage().volumes().create(builder.build());
            Volume openstackVolume = os.blockStorage().volumes().get(volume.getId());
            VolumeInfo2 volumeInfo2 = new VolumeInfo2(openstackVolume);
            list2.add(volumeInfo2);
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
    public DeleteInfo deleteVolume(CredentialInfo credentialInfo, String projectId, String volumeId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);
        Volume openstackVolume = os.blockStorage().volumes().get(volumeId);
        ActionResponse ar = os.blockStorage().volumes().delete(volumeId);

        VolumeInfo volumeinfo = new VolumeInfo(openstackVolume);

        if (ar != null && ar.isSuccess()) {
            DeleteInfo deleteinfo = new DeleteInfo();
            deleteinfo.setId(volumeinfo.getId());
            deleteinfo.setName(volumeinfo.getName());
            return deleteinfo;
        } else {
            throw new NullPointerException(ar.getFault());
        }
    }

    @Override
    public List<? extends VolumeType> getVolumeTypes(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, null);

        return os.blockStorage().volumes().listVolumeTypes();
    }

    @Override
    public List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, String projectId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);


        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Network> openstackList = os.networking().network().list(new HashMap<String, String>(){{
            if(projectId != null) {
                put("project_id", projectId);
            }
        }});

        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();

        if(webCheck) {
            for (int j = 0; j < openstackList.size(); j++) {
                Network network = openstackList.get(j);

                NetworkInfo info = new NetworkInfo(network);

                if (info.getProjectId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
                }

                list.add(info);
            }
            return list;
        }
        else {
            for (int j = 0; j < openstackList.size(); j++) {
                Network network = openstackList.get(j);

                NetworkInfo2 info = new NetworkInfo2(network);

                list2.add(info);
            }

        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if(webCheck) {
            return jsonArray;
        }
        else return jsonArray2;
    }

    @Override
    public List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String projectId, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Network> openstackList = os.networking().network().list(new HashMap<String, String>(){{
            if(projectId != null) {
                put("project_id", projectId);
            }
        }});

        List<NetworkInfo2> list2 = new ArrayList<>();

        for (int j = 0; j < openstackList.size(); j++) {
            Network network = openstackList.get(j);

            NetworkInfo2 info = new NetworkInfo2(network);

            if(type.equals("name")){
                if(info.getName().equals(value)) list2.add(info);
            }
            if(type.equals("networkState")){
                if(info.getState().equals(value)) list2.add(info);
            }
        }

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        return jsonArray2;
    }

    @Override
    public List<NetworkInfo> getNetwork(CredentialInfo credentialInfo, String projectId, String networkId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Network network = os.networking().network().get(networkId);


        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        if (webCheck) {
            NetworkInfo info = new NetworkInfo(network);

            if (info.getId() == null || info.getId().equals("")) {
                info.setId(networkId);
            }

            if (info.getProjectId() != null) {
                info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
            }
            for (int i = 0; i < info.getId().length(); i++) {

                if (info.getId() != null && info.getId().equals(networkId)) {
                    list.add(info);
                    break;
                } else {
                }
            }
        } else {
            NetworkInfo2 info = new NetworkInfo2(network);

            if (info.getId() == null || info.getId().equals("")) {
                info.setId(networkId);
            }

            for (int i = 0; i < info.getId().length(); i++) {

                if (info.getId() != null && info.getId().equals(networkId)) {
                    list2.add(info);
                    break;
                } else {
                }
            }
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
        } else return jsonArray2;
    }

    @Override
    public Object createNetwork(CredentialInfo credentialInfo, String projectId, CreateNetworkInfo createNetworkInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();
        String jsonString = null;
        String jsonString2 = null;
        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        if(webCheck) {
            NetworkBuilder builder = Builders.network()
                    .name(createNetworkInfo.getName())
                    .tenantId(createNetworkInfo.getTenantId())
                    .isShared(createNetworkInfo.getShared())
                    .adminStateUp(createNetworkInfo.getAdminStateUp());

            if (createNetworkInfo.getAvailabilityZones() != null) {
                int zoneSize = createNetworkInfo.getAvailabilityZones().length;
                for (int i = 0; i < zoneSize; i++) {
                    builder.addAvailabilityZoneHints(createNetworkInfo.getAvailabilityZones()[i]);
                }
            }

            Network network = os.networking().network()
                    .create(builder.build());

            list=getNetwork(credentialInfo, projectId, network.getId(), true);
        }
        else{
            NetworkBuilder builder = Builders.network()
                    .name(createNetworkInfo.getName())
                    .tenantId(createNetworkInfo.getTenantId())
                    .isShared(createNetworkInfo.getNetworkShared())
                    .adminStateUp(createNetworkInfo.getNetworkManaged());

            if (createNetworkInfo.getAvailabilityZones() != null) {
                int zoneSize = createNetworkInfo.getAvailabilityZones().length;
                for (int i = 0; i < zoneSize; i++) {
                    builder.addAvailabilityZoneHints(createNetworkInfo.getAvailabilityZones()[i]);
                }
            }

            Network network = os.networking().network()
                    .create(builder.build());
            Network network2 = os.networking().network().get(network.getId());
            NetworkInfo2 info= new NetworkInfo2(network2);
            list2.add(info);
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
            return jsonArray2;
        }

    }

    @Override
    public DeleteInfo delete(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);
        Server openstackServer = os.compute().servers().get(serverId);
        ActionResponse ar = os.compute().servers().delete(serverId);
        ServerInfo serverInfo = new ServerInfo(openstackServer);

        if (ar != null && ar.isSuccess()) {
            DeleteInfo deleteinfo = new DeleteInfo();
            deleteinfo.setId(serverInfo.getId());
            deleteinfo.setName(serverInfo.getName());
            return deleteinfo;
        } else {
            throw new NullPointerException(ar.getFault());
        }
    }

    @Override
    public DeleteInfo deleteNetwork(CredentialInfo credentialInfo, String projectId, String networkId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Network network = os.networking().network().get(networkId);
        ActionResponse ar = os.networking().network().delete(networkId);
        NetworkInfo networkInfo=new NetworkInfo(network);

        if (ar != null && ar.isSuccess()) {
            DeleteInfo deleteinfo = new DeleteInfo();
            deleteinfo.setId(networkInfo.getId());
            deleteinfo.setName(networkInfo.getName());
            return deleteinfo;
        } else {
            throw new NullPointerException(ar.getFault());
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
                credentialInfo.setType(list.get(i).getType() == "openstack" ? "3" : "3");
                credentialInfo.setDomain(list.get(i).getDomain());
                credentialInfo.setUrl(list.get(i).getUrl());
                credentialInfo.setTenant(list.get(i).getTenant());
                credentialInfo.setAccessId(list.get(i).getAccessId());
                credentialInfo.setAccessToken(list.get(i).getAccessToken());
                credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
                credentialInfo.setProjects(list.get(i).getProjects());
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
    public ProjectInfo getProject(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        List<ProjectInfo> projectInfos = getProjectsInMemory(credentialInfo);
        List<ProjectInfo> result = projectInfos.stream().filter(project -> project.getId().equals(projectId)).collect(Collectors.toList());
        if(result.size() > 0) {
            return result.get(0);
        }
        return new ProjectInfo();
    }

    @Override
    public Object createServer(CredentialInfo credentialInfo, String projectId, CreateServerInfo createServerInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        if(webCheck) {
            String name = createServerInfo.getName();
            String zone = createServerInfo.getZone();
            String type = createServerInfo.getSourceType();
            String id = createServerInfo.getSourceId();
            String flavor = createServerInfo.getFlavorId();
            List<String> networks = createServerInfo.getNetworks();
            List<String> securityGroups = createServerInfo.getSecurityGroups();
            String keyPair = createServerInfo.getKeyPair();
            Boolean configDrive = createServerInfo.getConfigDrive();
            String script = createServerInfo.getScript();
            Boolean deleteOnTermination = createServerInfo.getDeleteOnTermination();
            Boolean newVolume = createServerInfo.getNewVolume();
            Integer size = createServerInfo.getSize();

            ServerCreateBuilder sc = Builders.server().name(name).flavor(flavor).keypairName(keyPair);

            if(zone != null) {
                sc.availabilityZone(zone);
            }
            if(type.equals("image")) {
                sc.image(id);

                if(newVolume) {
                    BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping()
                            .uuid(id)
                            .sourceType(BDMSourceType.IMAGE)
                            .deviceName("/dev/vda")
                            .volumeSize(size)
                            .bootIndex(0).destinationType(BDMDestType.LOCAL);

                    if(deleteOnTermination) {
                        blockDeviceMappingBuilder.deleteOnTermination(true);
                    }

                    sc.blockDevice(blockDeviceMappingBuilder.build());
                }

            } else if(type.equals("volume")) {
                BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping()
                        .uuid(id)
                        .sourceType(BDMSourceType.VOLUME)
                        .deviceName("/dev/vda")
                        .bootIndex(0).destinationType(BDMDestType.LOCAL);

                if(deleteOnTermination) {
                    blockDeviceMappingBuilder.deleteOnTermination(true);
                }

                sc.blockDevice(blockDeviceMappingBuilder.build());
            }

            if(sc == null) return new ServerInfo();

            for (String securityGroup : securityGroups) {
                sc.addSecurityGroup(securityGroup);
            }
            if (networks != null && !networks.isEmpty()) {
                sc.networks(networks);
            }

            if(configDrive != null) {
                sc.configDrive(configDrive);
            }

            if(script != null) {
                try {
                    script = Base64.getEncoder().encodeToString(script.getBytes("UTF-8"));
                    sc.userData(script);
                } catch (UnsupportedEncodingException uee) {
                    logger.error("Failed to encode string to UTF-8 : '{}'", uee.getMessage());
                }
            }

            Server server = os.compute().servers().boot(sc.build());

            list=getServer(credentialInfo, projectId, server.getId(), true);
        }
        else{
            String name = createServerInfo.getName();
            String zone = createServerInfo.getZone();
            String type = createServerInfo.getSourceType();
            String id = createServerInfo.getImageId();
            String flavor = createServerInfo.getFlavorName();

            List<String> networks = createServerInfo.getNetworkId();
            List<String> securityGroup = createServerInfo.getSecurityGroupName();


            String keypair = createServerInfo.getKeyPair();
            Boolean configDrive = createServerInfo.getConfigDrive();
            String script = createServerInfo.getScript();
            Boolean deleteOnTermination = createServerInfo.getDeleteOnTermination();
            Boolean newVolume = createServerInfo.getVolumeCreated();
            Integer size = createServerInfo.getSize();

            List<FlavorInfo> flavorInfos = getFlavors(credentialInfo, projectId);
            for(FlavorInfo temp : flavorInfos){
                if(temp.getName().equals(createServerInfo.getFlavorName())){
                    flavor = temp.getId();
                    break;
                }
            }

            ServerCreateBuilder sc = Builders.server().name(name).flavor(flavor).keypairName(keypair);

            if (zone != null) {
                sc.availabilityZone(zone);
            }
            if (type.equals("image")) {
                sc.image(id);

                if (newVolume) {
                    BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping()
                            .uuid(id)
                            .sourceType(BDMSourceType.IMAGE)
                            .deviceName("/dev/vda")
                            .volumeSize(size)
                            .bootIndex(0).destinationType(BDMDestType.LOCAL);

                    sc.blockDevice(blockDeviceMappingBuilder.build());
                }

            } else if (type.equals("volume")) {
                BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping()
                        .uuid(id)
                        .sourceType(BDMSourceType.VOLUME)
                        .deviceName("/dev/vda")
                        .bootIndex(0).destinationType(BDMDestType.LOCAL);

                if (deleteOnTermination) {
                    blockDeviceMappingBuilder.deleteOnTermination(true);
                }

                sc.blockDevice(blockDeviceMappingBuilder.build());
            }

            if (sc == null) return new ServerInfo();

            for (String securityGroups : securityGroup) {
                sc.addSecurityGroup(securityGroups);
            }
            if (networks != null && !networks.isEmpty()) {
                sc.networks(networks);
            }

            if (configDrive != null) {
                sc.configDrive(configDrive);
            }

            if (script != null) {
                try {
                    script = Base64.getEncoder().encodeToString(script.getBytes("UTF-8"));
                    sc.userData(script);
                } catch (UnsupportedEncodingException uee) {
                    logger.error("Failed to encode string to UTF-8 : '{}'", uee.getMessage());
                }
            }

            Server server = os.compute().servers().boot(sc.build());
            Server openstackServer = os.compute().servers().get(server.getId());
            List<NetworkInfo> networkInfoList= getNetworks(credentialInfo, projectId, true);
            ServerInfo2 serverInfo2= new ServerInfo2(openstackServer,networkInfoList);
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
    public List<FlavorInfo> getFlavors(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Flavor> openstackFlavors = os.compute().flavors().list();

        List<FlavorInfo> list = new ArrayList<>();
        for(int j=0; j<openstackFlavors.size(); j++) {
            Flavor flavor = openstackFlavors.get(j);

            FlavorInfo info = new FlavorInfo(flavor);

            list.add(info);
        }
        return list;
    }

    @Override
    public String getProjectName(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        List<ProjectInfo> projectInfos = getProjectsInMemory(credentialInfo);

        List<ProjectInfo> result = projectInfos.stream().filter(project -> project.getId().equals(projectId)).collect(Collectors.toList());
        if(result.size() > 0) {
            return result.get(0).getName();
        }

        return "";
    }

    @Override
    public List<ProjectInfo> getProjectsInMemory(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        List<ProjectInfo> projectInfos = projectMap.get(credentialInfo.getId());

        if(projectInfos != null) {
            return projectInfos;
        } else {
            return getProjects(credentialInfo);
        }
    }

    @Override
    public List<ProjectInfo> getProjects(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        List<ProjectInfo> list = new ArrayList<>();

        try {
            OSClient os = getOpenstackClient(credentialInfo, null);

            List<? extends Project> openstackList = ((OSClient.OSClientV3) os).identity().projects().list();


            for (int j = 0; j < openstackList.size(); j++) {
                Project project = openstackList.get(j);

                ProjectInfo info = new ProjectInfo(project);

                list.add(info);
            }
        } catch (AuthenticationException e) {
            logger.error("Failed to getProjects : '{}'", e.getMessage());
        } catch (ClientResponseException e) {
            logger.error("Failed to getProjects : '{}'", e.getMessage());
        }

        projectMap.put(credentialInfo.getId(), list);

        return list;
    }

    @Override
    public List<ImageInfo> getImages(CredentialInfo credentialInfo, String projectId, Boolean active, Object imageModel){
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends org.openstack4j.model.image.v2.Image> openstackImages = os.imagesV2().list(new HashMap<String, String>(){{
            if(active) {
                put("status", "active");
            }
            if(imageModel != null){
                String string_serverModel = imageModel.toString();
                int lineCnt = 0;
                int fromIndex = -1;
                while ((fromIndex = string_serverModel.indexOf(":", fromIndex + 1)) >= 0) {
                    lineCnt++;
                }

                JSONArray jsonArray_model = JSONArray.fromObject(imageModel);

                JSONObject jsonObj = jsonArray_model.getJSONObject(0);

                Set key = jsonObj.keySet();

                Iterator iter = key.iterator();

                while(iter.hasNext()){
                    Object keyName = iter.next();

                    put((String) keyName, jsonObj.getString((String) keyName));
                }
            }
        }});

        List<ImageInfo> list = new ArrayList<>();
        for(int j=0; j<openstackImages.size(); j++) {
            Image image = openstackImages.get(j);

            ImageInfo info = new ImageInfo(image);

            list.add(info);
        }

        getAllImages(credentialInfo, projectId);

        return list;
    }

    @Override
    public List<NovaImageInfo> getAllImages(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends org.openstack4j.model.compute.Image> images = os.compute().images().list();

        List<NovaImageInfo> list = new ArrayList<>();

        for(org.openstack4j.model.compute.Image image : images){
            list.add(new NovaImageInfo(image));
        }

        return list;
    }


    @Override
    public void deleteCredential(CredentialInfo credentialInfo, String projectId, String credentialId, CredentialDao credentialDao) {
        if (credentialInfo == null) throw new CredentialException();
        if (credentialInfo.getType().equals(credentialId)){
            credentialDao.deleteCredential(credentialInfo);
        }else{
            throw new NullPointerException();
        }
    }
}
