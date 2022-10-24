package com.datahub.infra.apiopenstack.service.impl;

import com.datahub.infra.apiopenstack.service.OpenStackService;
import com.datahub.infra.core.exception.CityHubUnAuthorizedException;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coreopenstack.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.exceptions.ServerResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.HypervisorStatistics;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.image.v2.ContainerFormat;
import org.openstack4j.model.image.v2.DiskFormat;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.image.v2.builder.ImageBuilder;
import org.openstack4j.model.network.*;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.builder.NetworkBuilder;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeBackup;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.VolumeType;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.openstack4j.openstack.OSFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

        List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
        }});

        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        if(webCheck) {
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

            List<NetworkInfo> networkInfoList= getNetworks(credentialInfo,projectId,true);

            List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false);

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

            List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false);
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

                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false);
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
    public List<ImageInfo> getImages(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        return getImages(credentialInfo, projectId, false);
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
    public List<ImageInfo> getImages(CredentialInfo credentialInfo, String projectId, Boolean active) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Image> openstackImages = os.imagesV2().list(new HashMap<String, String>(){{
            if(active) {
                put("status", "active");
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
    public List<KeyPairInfo> getKeyPairs(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Keypair> openstackList = os.compute().keypairs().list();

        List<KeyPairInfo> list = new ArrayList<>();
        for(int j=0; j<openstackList.size(); j++) {
            Keypair keypair = openstackList.get(j);

            KeyPairInfo info = new KeyPairInfo(keypair);

            list.add(info);
        }

        return list;
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
    public List<FlavorInfo> getFlavor(CredentialInfo credentialInfo, String flavorId, String projectId) {
        if (credentialInfo == null) throw new CredentialException();
        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Flavor> openstackFlavors = os.compute().flavors().list();

        List<FlavorInfo> list = new ArrayList<>();

        for (Flavor flavor : openstackFlavors) {
            FlavorInfo info = new FlavorInfo(flavor);
            if (info.getId() != null && info.getId().equals(flavorId)) {
                list.add(info);
                break;
            }
        }
        return list;
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
                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, webCheck);

                for (int j = 0; j < openstackvolumes.size(); j++) {
                    Volume volume = openstackvolumes.get(j);

                    VolumeInfo info = new VolumeInfo(volume);

                    list.add(info);
                }
                return list;
            }
        } else {
            if (openstackvolumes.size() > 0) {
                List<ImageInfo> imageInfos =  getImages(credentialInfo, projectId, false);
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
    public List<VolumeBackupInfo> getBackups(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends VolumeBackup> openstackLists = os.blockStorage().backups().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
        }});

        List<VolumeBackupInfo> list = new ArrayList<>();

        if(openstackLists.size() > 0) {
            List<VolumeInfo> volumeInfos = getVolumes(credentialInfo, projectId, false);

            for (int j = 0; j < openstackLists.size(); j++) {
                VolumeBackup openstack = openstackLists.get(j);

                VolumeBackupInfo info = new VolumeBackupInfo(openstack);
                List<VolumeInfo> result = volumeInfos.stream().filter(volume -> volume.getId().equals(info.getVolumeId())).collect(Collectors.toList());
                if (result.size() > 0) {
                    info.setVolumeName(result.get(0).getName());
                }
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public List<VolumeSnapshotInfo> getSnapshots(CredentialInfo credentialInfo, String projectId, Boolean available) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends VolumeSnapshot> openstackLists = os.blockStorage().snapshots().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
            if(available) {
                put("status", "available");
            }
        }});
        List<VolumeSnapshotInfo> list = new ArrayList<>();

        if(openstackLists.size() > 0) {

            List<VolumeInfo> volumeInfos = getVolumes(credentialInfo, projectId, true);

            for (int j = 0; j < openstackLists.size(); j++) {
                VolumeSnapshot openstack = openstackLists.get(j);

                VolumeSnapshotInfo info = new VolumeSnapshotInfo(openstack);
                List<VolumeInfo> result = volumeInfos.stream().filter(volume -> volume.getId().equals(info.getVolumeId())).collect(Collectors.toList());
                if (result.size() > 0) {
                    info.setVolumeName(result.get(0).getName());
                }

                list.add(info);
            }
        }

        return list;
    }


    @Override
    public List<VolumeSnapshotInfo> getSnapshotid(CredentialInfo credentialInfo, String snapshotId, String projectId, Boolean available){

        if (credentialInfo == null) throw new CredentialException();
        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends VolumeSnapshot> openstackLists = os.blockStorage().snapshots().list(new HashMap<String, String>(){{
            if(projectId == null) {
                put("all_tenants", "true");
            }
            if(available) {
                put("status", "available");
            }
        }});
        List<VolumeSnapshotInfo> list = new ArrayList<>();

        if(openstackLists.size() > 0) {
            List<VolumeInfo> volumeInfos = getVolumes(credentialInfo, projectId, true);

            for (int j = 0; j < openstackLists.size(); j++) {
                VolumeSnapshot openstack = openstackLists.get(j);

                VolumeSnapshotInfo info = new VolumeSnapshotInfo(openstack);
                List<VolumeInfo> result = volumeInfos.stream().filter(volume -> volume.getId().equals(info.getVolumeId())).collect(Collectors.toList());

                if (info.getId() != null && info.getId().equals(snapshotId)) {
                    if (result.size() > 0) {
                        info.setVolumeName(result.get(0).getName());
                    }
                    list.add(info);
                    break;
                }else{
                    throw new NullPointerException("Null Pointer Exception");
                }

            }
        }

        return list;
    }

    @Override
    public VolumeSnapshotInfo getSnapshot(CredentialInfo credentialInfo, String projectId, String snapshotId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        VolumeSnapshot snapshot = os.blockStorage().snapshots().get(snapshotId);
        VolumeSnapshotInfo info = new VolumeSnapshotInfo(snapshot);

        if(info.getId() == null || info.getId().equals("")) {
            info.setId(snapshotId);
        }

        return info;
    }

    @Override
    public VolumeSnapshotInfo createSnapsthot(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String name = "";
        String description = "";
        String volumeId = "";

        VolumeSnapshot snapshot =os.blockStorage().snapshots()
                .create(Builders.volumeSnapshot()
                        .name(name)
                        .description(description)
                        .volume(volumeId)
                        .build());

        return getSnapshot(credentialInfo, projectId, snapshot.getId());
    }

    @Override
    public VolumeSnapshotInfo deleteSnapshot(CredentialInfo credentialInfo, String projectId, String snapshotId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.blockStorage().snapshots().delete(snapshotId);

        if(ar != null && ar.isSuccess()) {
            VolumeSnapshotInfo info = new VolumeSnapshotInfo();
            info.setId(snapshotId);
            return info;
        } else {
            return new VolumeSnapshotInfo();
        }
    }

    @Override
    public VolumeSnapshotInfo updateSnapshot(CredentialInfo credentialInfo, String projectId, String snapshotId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String name = "";
        String description = "";

        ActionResponse ar = os.blockStorage().snapshots().update(snapshotId, name, description);

        if(ar != null && ar.isSuccess()) {
            return getSnapshot(credentialInfo, projectId, snapshotId);
        } else {
            return new VolumeSnapshotInfo();
        }
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
    public List<RouterInfo> getRouters(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);


        List<? extends Network> openstackNetwork = os.networking().network().list(new HashMap<String, String>(){{
            if(projectId != null) {
                put("project_id", projectId);
            }
        }});

        List<NetworkInfo> networks = new ArrayList<>();
        for(int j=0; j<openstackNetwork.size(); j++) {
            Network network = openstackNetwork.get(j);

            NetworkInfo info = new NetworkInfo(network);

            if(info.getProjectId() != null) {
                info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
            }

            networks.add(info);
        }


        List<? extends Router> openstackList = null;

        if(projectId == null) {
            openstackList = os.networking().router().list();
        } else {
            openstackList = os.networking().router().list().stream().filter(r -> r.getTenantId().equals(projectId)).collect(Collectors.toList());
        }
        List<RouterInfo> list = new ArrayList<>();

        if(openstackList.size() > 0) {

            for (int j = 0; j < openstackList.size(); j++) {
                Router router = openstackList.get(j);

                RouterInfo info = new RouterInfo(router);

                if (router.getExternalGatewayInfo() != null && networks != null) {
                    List<NetworkInfo> result = networks.stream().filter(network -> network.getId().equals(info.getNetworkId())).collect(Collectors.toList());
                    if (result.size() > 0) info.setNetworkName(result.get(0).getName());
                    if (result.size() > 0) info.setVisibilityZones(result.get(0).getVisibilityZones());
                }

                if (info.getProjectId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
                }

                list.add(info);
            }
        }

        return list;
    }

    @Override
    public RouterInfo getRouter(CredentialInfo credentialInfo, String projectId, String routerId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Router router = os.networking().router().get("routerId");
        RouterInfo info = new RouterInfo(router);

        if(info.getId() == null || info.getId().equals("")) {
            info.setId(routerId);
        }

        if(info.getProjectId() != null) {
            info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
        }

        return info;
    }

    @Override
    public RouterInfo createRouter(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String name = "";
        String networkId = "";
        String[] routers = {};

        Router router = os.networking().router().create(Builders.router()
                .name(name)
                .adminStateUp(true)
                .externalGateway(networkId)
                .route(routers[0], routers[1])
                .build());

        return getRouter(credentialInfo, projectId, router.getId());
    }

    @Override
    public RouterInfo deleteRouter(CredentialInfo credentialInfo, String projectId, String routerId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.networking().router().delete(routerId);

        if(ar != null && ar.isSuccess()) {
            return getRouter(credentialInfo, projectId, routerId);
        } else {
            return new RouterInfo();
        }
    }

    @Override
    public RouterInfo updateRouter(CredentialInfo credentialInfo, String projectId, String routerId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String name = "";
        Router router = os.networking().router().get(routerId);
        router = os.networking().router().update(router.toBuilder().name(name).build());

        return getRouter(credentialInfo, projectId, router.getId());
    }

    @Override
    public List<SubnetInfo> getSubnets(CredentialInfo credentialInfo, String projectId, String networkId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends Subnet> subnetList =  os.networking().subnet().list();

        List<SubnetInfo> list = new ArrayList<>();
        for(int j=0; j<subnetList.size(); j++) {
            Subnet subnet = subnetList.get(j);

            if(networkId != null && !subnet.getNetworkId().equals(networkId)) {
                continue;
            }

            SubnetInfo info = new SubnetInfo(subnet);

            list.add(info);
        }

        return list;
    }

    @Override
    public SubnetInfo getSubnet(CredentialInfo credentialInfo, String projectId, String subnetId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Subnet subnet = os.networking().subnet().get(subnetId);
        SubnetInfo info = new SubnetInfo(subnet);

        if(info.getId() == null || info.getId().equals("")) {
            info.setId(subnetId);
        }

        /*if(info.getProjectId() != null) {
            info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
        }*/

        return info;
    }

    @Override
    public SubnetInfo createSubnet(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String name = "";
        String tenantId = "";
        String networkId = "";
        String cidr = "";
        String[] pools = {};

        Subnet subnet = os.networking().subnet().create(Builders.subnet()
                .name(name)
                .networkId(networkId)
                .tenantId(tenantId)
                .addPool(pools[0], pools[1])
                .ipVersion(IPVersionType.V4)
                .cidr(cidr)
                .build());

        return getSubnet(credentialInfo, projectId, subnet.getId());
    }

    @Override
    public SubnetInfo deleteSubnet(CredentialInfo credentialInfo, String projectId, String subnetId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.networking().subnet().delete(subnetId);

        if(ar != null && ar.isSuccess()) {
            return getSubnet(credentialInfo, projectId, subnetId);
        } else {
            return new SubnetInfo();
        }
    }

    @Override
    public List<SecurityGroupInfo> getSecurityGroups(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();



        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends SecurityGroup> openstackList = os.networking().securitygroup().list(new HashMap<String, String>(){{
            if(projectId != null) {
                put("project_id", projectId);
            }
        }});

        List<SecurityGroupInfo> list = new ArrayList<>();
        for(int j=0; j<openstackList.size(); j++) {
            SecurityGroup securityGroup = openstackList.get(j);

            SecurityGroupInfo info = new SecurityGroupInfo(securityGroup);

            list.add(info);
        }

        return list;
    }

    @Override
    public List<FloatingIpInfo> getFloatingIps(CredentialInfo credentialInfo, String projectId, Boolean down) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends NetFloatingIP> openstackList = os.networking().floatingip().list(new HashMap<String, String>(){{
            if(projectId != null){
                put("project_id", projectId);
            }
            if(down) {
                put("status", "down");
            }
        }});
        List<FloatingIpInfo> list = new ArrayList<>();

        if(openstackList.size() > 0) {
            List<ServerInfo> serverInfoList = getServers(credentialInfo, projectId, true);
            List<NetworkInfo> networks = getNetworks(credentialInfo, projectId, true);

            for (int i = 0; i < openstackList.size(); i++) {
                NetFloatingIP floatingIP = openstackList.get(i);

                FloatingIpInfo info = new FloatingIpInfo(floatingIP);
                if (info.getFixedIpAddress() != null && !info.getFixedIpAddress().equals("")) {
                    AddressInfo address;
                    for (int j = 0; j < serverInfoList.size(); j++) {
                        address = serverInfoList.get(j).getAddressInfo(info.getFixedIpAddress());
                        if (address != null) {
                            info.setServerName(serverInfoList.get(j).getName());
                            break;
                        }
                    }
                }

                List<NetworkInfo> result = networks.stream().filter(network -> network.getId().equals(info.getFloatingNetworkId())).collect(Collectors.toList());
                if (result.size() > 0) info.setNetworkName(result.get(0).getName());

                if (info.getTenantId() != null) {
                    info.setProjectName(getProjectName(credentialInfo, info.getTenantId()));
                }

                list.add(info);
            }
        }

        return list;
    }

    @Override
    public List<AvailabilityZoneInfo> getZones(CredentialInfo credentialInfo, String projectId, String type) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List openstackList = null;

        if(type.equals("compute")) {
            openstackList = os.compute().zones().list();
        } else if(type.equals("volume")) {
            openstackList = os.blockStorage().zones().list();
        } else if(type.equals("network")) {
            openstackList = os.networking().availabilityzone().list();
        }

        List<AvailabilityZoneInfo> list = new ArrayList<>();
        for(int i=0; i<openstackList.size(); i++) {
            AvailabilityZoneInfo info = new AvailabilityZoneInfo(openstackList.get(i));

            list.add(info);
        }

        return list;
    }

    @Override
    public List<String> getFloatingIpPoolNames(CredentialInfo credentialInfo, String projectId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<String> pools = os.compute().floatingIps().getPoolNames();

        return pools;
    }

    @Override
    public FloatingIpInfo allocateFloatingIp(CredentialInfo credentialInfo, String projectId, String poolName) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        FloatingIP ip = os.compute().floatingIps().allocateIP(poolName);

        FloatingIpInfo info = new FloatingIpInfo(ip);

        return info;
    }

    @Override
    public Boolean deallocateFloatingIp(CredentialInfo credentialInfo, String projectId, String floatingIpId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().floatingIps().deallocateIP(floatingIpId);

        if(ar.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean addFloatingIpToServer(CredentialInfo credentialInfo, String projectId, String serverId, String interfaceIp, String floatingIp) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().floatingIps().addFloatingIP(serverId, interfaceIp, floatingIp);

        if(ar.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean removeFloatingIpToServer(CredentialInfo credentialInfo, String projectId, String serverId, String floatingIp) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().floatingIps().removeFloatingIP(serverId, floatingIp);

        if(ar.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean attachInterface(CredentialInfo credentialInfo, String projectId, String serverId, String networkId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Port p = os.networking().port().create(Builders.port().networkId(networkId).build());

        InterfaceAttachment ia = os.compute().servers().interfaces().create(serverId, p.getId());

        if(ia != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean detachInterface(CredentialInfo credentialInfo, String projectId, String serverId, String portId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().interfaces().detach(serverId, portId);
        if(ar.isSuccess()) {
            ar = os.networking().port().delete(portId);
            if(ar.isSuccess()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public List<? extends InterfaceAttachment> getServerInterface(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends InterfaceAttachment> list = os.compute().servers().interfaces().list(serverId);

        return list;
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
    public ServerInfo start(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.START);

        if(ar != null && ar.isSuccess()) {
            return new ServerInfo(os.compute().servers().get(serverId));
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo stop(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.STOP);

        if(ar != null && ar.isSuccess()) {
            return new ServerInfo(os.compute().servers().get(serverId));
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo rebootSoft(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().reboot(serverId, RebootType.SOFT);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo rebootHard(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().reboot(serverId, RebootType.HARD);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
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
    public ServerInfo pause(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.PAUSE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo unpause(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.UNPAUSE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo lock(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.LOCK);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo unlock(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.UNLOCK);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo suspend(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.SUSPEND);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo resume(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.RESUME);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo rescue(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.RESCUE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo unrescue(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.UNRESCUE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo shelve(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.SHELVE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo shelveOffload(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.SHELVE_OFFLOAD);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
    }

    @Override
    public ServerInfo unshelve(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().action(serverId, Action.UNSHELVE);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            return new ServerInfo();
        }
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
                        .bootIndex(0);

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
            return jsonArray2;
        }

    }

    @Override
    public String createServerSnapshot(CredentialInfo credentialInfo, String projectId, String serverId, String snapshotName) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String imageId = os.compute().servers().createSnapshot(serverId, snapshotName);

        return imageId;
    }

    @Override
    public Object getServerMetric(CredentialInfo credentialInfo, RequestMetricInfo requestMetricInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Map<String, Object> dataList = new HashMap<>();
        String INFLUX_DATABASE = "openstackit";
        int INFLUX_DB_QUERY_PORT = 8086;
        int CHECK_CPU_BASIC = 0b0000000001;
        int CHECK_DISK_BASIC = 0b0000000010;
        int CHECK_NETWORK_BASIC = 0b0000000100;
        int CHECK_MEMORY_USAGE = 0b0000001000;
        int CHECK_MEMORY = 0b0000010000;
        int CHECK_CPU = 0b0000100000;
        int CHECK_DISK_USAGE = 0b0001000000;
        int CHECK_LOAD = 0b0010000000;
        int CHECK_PROCESS = 0b0100000000;
        int CHECK_SWAP_USAGE = 0b1000000000;

        String influxDBURL = "127.0.0.1";
        InfluxDB influxDB = InfluxDBFactory.connect("http://" + influxDBURL + ":" + INFLUX_DB_QUERY_PORT, "root", "root");

        Integer metricNum =  requestMetricInfo.getMetricName();
        String statistic = requestMetricInfo.getStatistic();
        Integer interval = requestMetricInfo.getInterval();
        Long endDate =  requestMetricInfo.getEndDate();
        Long startDate = requestMetricInfo.getStartDate();

        String function = "MEAN";

        if (!statistic.equals("")) {
            function = statistic;
        }

        try {
            List<String> chartType = new ArrayList<>();

            if ((metricNum & CHECK_CPU_BASIC) > 0) {
                chartType.add(String.format("%s(cpu_utilization) AS serverCpuUsageCPU", function));
            }
            if ((metricNum & CHECK_DISK_BASIC) > 0) {
                chartType.add(String.format("%s(bytes_read) AS serverDiskRead", function));
                chartType.add(String.format("%s(bytes_written) AS serverDiskWrite", function));
            }
            if ((metricNum & CHECK_NETWORK_BASIC) > 0) {
                chartType.add(String.format("%s(bytes_in) AS serverNetworkOutput", function));
                chartType.add(String.format("%s(bytes_out) AS serverNetworkInput", function));
            }
            if ((metricNum & CHECK_MEMORY_USAGE) > 0) {
                chartType.add(String.format("%s(mem_utilization) AS serverMemoryUsageMEM", function));
            }
            if ((metricNum & CHECK_MEMORY) > 0) {
                chartType.add(String.format("%s(mem_buffers) AS serverMemorybuffers", function));
                chartType.add(String.format("%s(mem_cached) AS serverMemorycached", function));
                chartType.add(String.format("%s(mem_free) AS serverMemoryfree", function));
                chartType.add(String.format("%s(mem_shared) AS serverMemoryshared", function));
            }
            if ((metricNum & CHECK_CPU) > 0) {
                chartType.add(String.format("%s(cpu_intr) AS serverCpuintr", function));
                chartType.add(String.format("%s(cpu_system) AS serverCpusystem", function));
                chartType.add(String.format("%s(cpu_user) AS serverCpuuser", function));
                chartType.add(String.format("%s(cpu_idle) AS serverCpuidle", function));
            }
            if ((metricNum & CHECK_DISK_USAGE) > 0) {
                chartType.add(String.format("%s(disk_utilization) AS serverDiskUsageDisk", function));
            }
            if ((metricNum & CHECK_LOAD) > 0) {
                chartType.add(String.format("%s(load_fifteen) AS serverLoad15minute", function));
                chartType.add(String.format("%s(load_five) AS serverLoad5minute", function));
                chartType.add(String.format("%s(load_one) AS serverLoad1minute", function));
            }
            if ((metricNum & CHECK_PROCESS) > 0) {
                chartType.add(String.format("%s(proc_run) AS serverProcessrun", function));
                chartType.add(String.format("%s(proc_total) AS serverProcesstotal", function));
            }
            if ((metricNum & CHECK_SWAP_USAGE) > 0) {
                chartType.add(String.format("(%s(swap_total)-%s(swap_free)) / %s(swap_total) * 100 AS serverSwapUsageSwap", function, function, function));
            }

            String columns = StringUtils.collectionToDelimitedString(chartType, ",");
            String queryString = String.format("SELECT %s FROM vm_stat WHERE uuid='%s' AND time >= %ds AND time < %ds GROUP BY time(%ds) ORDER BY time asc", columns, requestMetricInfo.getId(), startDate, endDate, interval);

            Query query = new Query(queryString, INFLUX_DATABASE);
            influxDB.setLogLevel(InfluxDB.LogLevel.FULL);
            QueryResult queryResult = influxDB.query(query, MILLISECONDS);

            List<QueryResult.Result> results = queryResult.getResults();
            if(results.get(0) != null ){
                List<QueryResult.Series> series = results.get(0).getSeries();
                if(series != null){
                    List<List<Object>> values = series.get(0).getValues();

                    for(String chart : chartType){
                        List<Object> d = new ArrayList<>();
                        for(List<Object> value : values){
                            int index = chartType.indexOf(chart)+1;
                            if(value.get(index) != null){
                                d.add(value.get(index));
                            }
                        }
                        if(chart.split(" ")[2].equals("MEAN(swap_total)") ){
                            dataList.put("serverSwapUsageSwap", d);
                        }else{
                            dataList.put(chart.split(" ")[2], d);
                        }
                    }
                }else{
                    for(String chart : chartType){
                        List<Object> d = new ArrayList<>();
                        if(chart.split(" ")[2].equals("MEAN(swap_total)") ){
                            dataList.put("serverSwapUsageSwap", d);
                        }else{
                            dataList.put(chart.split(" ")[2], d);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get ServerMetric : '{}'", e.getMessage());
        }
        influxDB.close();
        return dataList;
    }

    @Override
    public String getServerVNCConsoleURL(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        VNCConsole console = null;
        try {
            console = os.compute().servers().getVNCConsole(serverId, VNCConsole.Type.NOVNC);
        } catch (ClientResponseException cre) {
            logger.error("Fail getServerVNCConsoleURL - NOVNC {}", cre.getMessage());
            try {
                console = os.compute().servers().getVNCConsole(serverId, VNCConsole.Type.SPICE);
            } catch (ClientResponseException cre2) {
                logger.error("Fail getServerVNCConsoleURL - SPICE {}", cre2.getMessage());
            } catch (ServerResponseException sre) {
                logger.error("Fail getServerVNCConsoleURL {}", sre.getMessage());
            }
        } catch (ServerResponseException sre) {
            logger.error("Fail getServerVNCConsoleURL {}", sre.getMessage());
        }
        return console != null? console.getURL():"";
    }

    @Override
    public String getServerConsoleOutput(CredentialInfo credentialInfo, String projectId, String serverId, int line) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        String output = "";

        try {
            output = os.compute().servers().getConsoleOutput(serverId, line);
        } catch (ClientResponseException cre) {
            logger.error("Failed to get ServerConsoleOutput : '{}'", cre.getMessage());
        }
        return output;
    }

    @Override
    public List<ActionLogInfo> getServerActionLog(CredentialInfo credentialInfo, String projectId, String serverId) {
        if(credentialInfo == null) return Collections.emptyList();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<? extends InstanceAction> openstackList = os.compute().servers().instanceActions().list(serverId);

        List<ActionLogInfo> list = new ArrayList<>();
        for(int j=0; j<openstackList.size(); j++) {
            InstanceAction action = openstackList.get(j);

            ActionLogInfo info = new ActionLogInfo(action);

            list.add(info);
        }

        return list;
    }

    @Override
    public List<VolumeInfo> getServerVolumes(CredentialInfo credentialInfo, String projectId, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        List<VolumeInfo> list = new ArrayList<>();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        List<VolumeInfo> volumes = getVolumes(credentialInfo, projectId, true);

        if(volumes.size() > 0) {
            List<String> attachedVolumes = os.compute().servers().get(serverId).getOsExtendedVolumesAttached();

            for (int i = 0; i < volumes.size(); i++) {
                if (attachedVolumes.contains(volumes.get(i).getId())) {
                    list.add(volumes.get(i));
                }
            }
        }

        return list;
    }

    @Override
    public VolumeInfo detachVolume(CredentialInfo credentialInfo, String projectId, String serverId, String volumeId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().detachVolume(serverId, volumeId);

        if(ar != null && ar.isSuccess()) {
            Volume volume = os.blockStorage().volumes().get(volumeId);

            VolumeInfo info = new VolumeInfo(volume);

            if(info.getProjectId() != null) {
                info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
            }
            return info;
        } else {
            return new VolumeInfo();
        }
    }

    @Override
    public VolumeInfo attachVolume(CredentialInfo credentialInfo, String projectId, String serverId, String volumeId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        os.compute().servers().attachVolume(serverId, volumeId, null);

        Volume volume = os.blockStorage().volumes().get(volumeId);

        Server server = os.compute().servers().get(serverId);

        ServerInfo serverInfo = new ServerInfo(server);

        VolumeInfo info = new VolumeInfo(volume);

        if(info.getProjectId() != null) {
            info.setProjectName(getProjectName(credentialInfo, info.getProjectId()));
        }
        info.setServerNameForVolumeAttachmentInfos(Collections.singletonList(serverInfo));

        return info;
    }

    @Override
    public List<? extends Hypervisor> getHypervisors(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, null);

        return os.compute().hypervisors().list();
    }

    @Override
    public HypervisorStatistics getHypervisorStatistics(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, null);

        return os.compute().hypervisors().statistics();
    }

    @Override
    public ResourceInfo getResourceUsage(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, null);

        ResourceInfo info = new ResourceInfo();

        List<? extends Server> openstackServers = os.compute().servers().list(new HashMap<String, String>(){{
            put("all_tenants", "true");
        }});
        List<? extends Server> running = openstackServers.stream().filter(server -> server.getStatus() == Server.Status.ACTIVE).collect(Collectors.toList());
        List<? extends Server> stop = openstackServers.stream().filter(server -> server.getStatus() == Server.Status.SHUTOFF).collect(Collectors.toList());
        List<? extends Image> openstackImages = os.imagesV2().list(new HashMap<String, String>(){{}});
        List<? extends Flavor> openstackFlavors = os.compute().flavors().list();
        List<? extends Keypair> openstackKeyPairs = os.compute().keypairs().list();
        List<? extends Volume> openstackVolumes = os.blockStorage().volumes().list(new HashMap<String, String>(){{
            put("all_tenants", "true");
        }});
        List<? extends VolumeBackup> openstackBackups = os.blockStorage().backups().list(new HashMap<String, String>(){{
            put("all_tenants", "true");
        }});
        List<? extends VolumeSnapshot> openstackSnapshots = os.blockStorage().snapshots().list(new HashMap<String, String>(){{
            put("all_tenants", "true");
        }});
        List<? extends Network> openstackNetworks = os.networking().network().list(new HashMap<String, String>(){{

        }});
        List<? extends Router> openstackRouters = os.networking().router().list();
        List<? extends SecurityGroup> openstackSecurityGroups = os.networking().securitygroup().list(new HashMap<String, String>(){{}});
        List<? extends NetFloatingIP> openstackFloatingIps = os.networking().floatingip().list(new HashMap<String, String>(){{}});
        List<ProjectInfo> openstackProjects = getProjectsInMemory(credentialInfo);
        HypervisorStatistics hypervisorStatistics = os.compute().hypervisors().statistics();

        info.setRunning(running.size());
        info.setStop(stop.size());
        info.setEtc(openstackServers.size() - (running.size() + stop.size()));
        info.setImages(openstackImages.size());
        info.setFlavor(openstackFlavors.size());
        info.setKeyPairs(openstackKeyPairs.size());
        info.setVolumes(openstackVolumes.size());
        info.setBackups(openstackBackups.size());
        info.setSnapshots(openstackSnapshots.size());
        info.setNetworks(openstackNetworks.size());
        info.setRouters(openstackRouters.size());
        info.setSecurityGroups(openstackSecurityGroups.size());
        info.setFloatingIps(openstackFloatingIps.size());
        info.setProjects(openstackProjects.size());
        info.setHypervisorVcpus(hypervisorStatistics.getVirtualCPU());
        info.setHypervisorVcpusUsed(hypervisorStatistics.getVirtualUsedCPU());
        info.setHypervisorMemory(hypervisorStatistics.getMemory());
        info.setHypervisorMemoryUsed(hypervisorStatistics.getMemoryUsed());
        info.setHypervisorDisk(hypervisorStatistics.getLocal());
        info.setHypervisorDiskUsed(hypervisorStatistics.getLocalUsed());

        return info;
    }

    @Override
    public ImageInfo createImage(CredentialInfo credentialInfo, String projectId, CreateImageInfo createImageInfo) throws MalformedURLException {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ImageBuilder builder = Builders.imageV2()
                .name(createImageInfo.getName())
                .isProtected(createImageInfo.getProtect())
                .minDisk(createImageInfo.getMinDisk())
                .minRam(createImageInfo.getMinRam());

        if(createImageInfo.getVisibility().equalsIgnoreCase("public")) {
            builder.visibility(Image.ImageVisibility.PUBLIC);
        } else if(createImageInfo.getVisibility().equalsIgnoreCase("private")){
            builder.visibility(Image.ImageVisibility.PRIVATE);
        } else {
            builder.visibility(Image.ImageVisibility.UNKNOWN);
        }

        builder.containerFormat(ContainerFormat.BARE);

        if(createImageInfo.getFormat().equalsIgnoreCase("aki")) {
            builder.containerFormat(ContainerFormat.AKI);
            builder.diskFormat(DiskFormat.AKI);
        } else if(createImageInfo.getFormat().equalsIgnoreCase("ami")) {
            builder.containerFormat(ContainerFormat.AMI);
            builder.diskFormat(DiskFormat.AMI);
        } else if(createImageInfo.getFormat().equalsIgnoreCase("ari")) {
            builder.containerFormat(ContainerFormat.ARI);
            builder.diskFormat(DiskFormat.ARI);
        } else if(createImageInfo.getFormat().equalsIgnoreCase("docker")) {
            builder.containerFormat(ContainerFormat.DOCKER);
            builder.diskFormat(DiskFormat.RAW);
        } else if(createImageInfo.getFormat().equalsIgnoreCase("vhd")) {
            builder.containerFormat(ContainerFormat.OVF);
            builder.diskFormat(DiskFormat.VHD);
        } else {
            builder.diskFormat(DiskFormat.value(createImageInfo.getFormat()));
        }

        Image image = os.imagesV2().create(builder.build());

        Payload<URL> payload = Payloads.create(new URL(createImageInfo.getUrl()));

        ActionResponse ar = os.imagesV2().upload(image.getId(), payload, image);

        if (ar == null || !ar.isSuccess()) {
            logger.error("Failed to image upload : '{}'", ar.getFault());
        }

        return getImage(credentialInfo, projectId, image.getId());
    }

    @Override
    public ImageInfo getImage(CredentialInfo credentialInfo, String projectId, String imageId) {
        if (credentialInfo == null) throw new CredentialException();

        if(imageId == null) return new ImageInfo();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Image image = os.imagesV2().get(imageId);

        ImageInfo info = new ImageInfo(image);

        if(info.getId() == null || info.getId().equals("")) {
            info.setId(imageId);
        }

        return info;
    }

    @Override
    public ImageInfo deleteImage(CredentialInfo credentialInfo, String projectId, String imageId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.imagesV2().delete(imageId);

        if(ar != null && ar.isSuccess()) {
            return getImage(credentialInfo, projectId, imageId);
        } else {
            logger.error("Failed to deleteImage : '{}'", ar.getFault());

            return new ImageInfo();
        }
    }

    @Override
    public KeyPairInfo createKeypair(CredentialInfo credentialInfo, String projectId, KeyPairInfo keyPairInfo) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        Keypair kp = os.compute().keypairs().create(keyPairInfo.getName(), keyPairInfo.getPublicKey());

        return new KeyPairInfo(kp);
    }

    @Override
    public KeyPairInfo deleteKeypair(CredentialInfo credentialInfo, String projectId, String keypairName) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().keypairs().delete(keypairName);

        if(ar != null && ar.isSuccess()) {
            KeyPairInfo info = new KeyPairInfo();
            info.setName(keypairName);

            return info;
        } else {
            logger.error("Failed to deleteKeypair : '{}'", ar.getFault());

            return new KeyPairInfo();
        }

    }

    @Override
    public ServerInfo changeFlavor(CredentialInfo credentialInfo, String projectId, String serverId, String flavorId) {
        if (credentialInfo == null) throw new CredentialException();

        OSClient os = getOpenstackClient(credentialInfo, projectId);

        ActionResponse ar = os.compute().servers().resize(serverId, flavorId);

        if(ar != null && ar.isSuccess()) {
            return (ServerInfo) getServer(credentialInfo, projectId, serverId, true);
        } else {
            logger.error("Failed to changeFlavor : '{}'", ar.getFault());

            return new ServerInfo();
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
    public void deleteCredential(CredentialInfo credentialInfo, String projectId, String credentialId, CredentialDao credentialDao) {
        if (credentialInfo == null) throw new CredentialException();
        if (credentialInfo.getType().equals(credentialId)){
            credentialDao.deleteCredential(credentialInfo);
        }else{
            throw new NullPointerException();
        }
    }
}
