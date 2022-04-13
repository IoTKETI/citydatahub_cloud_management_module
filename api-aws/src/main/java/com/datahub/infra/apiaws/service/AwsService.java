package com.datahub.infra.apiaws.service;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coreaws.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;

import java.util.List;
import java.util.Map;

public interface AwsService {

    List<ServerInfo> getServers(CredentialInfo credentialInfo, Boolean webCheck);

    List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String value, String type);

    List<ServerInfo> getServer(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    Object createServer(CredentialInfo credentialInfo, CreateServerInfo createServerInfo, Boolean webCheck);

    Object delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    Object createVolume(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    List<VolumeInfo> getVolumes(CredentialInfo credentialInfo,Boolean webCheck);

    List<VolumeInfo> getVolumes_Search(CredentialInfo credentialInfo, String value, String type);

    List<VolumeInfo> getVolume(CredentialInfo credentialInfo, String volumeId,Boolean webCheck);

    Object deleteVolume(CredentialInfo credentialInfo, String volumeId, Boolean webCheck);

    List<NetworkInfo> getNetworks(CredentialInfo credentialInfo,Boolean webCheck);

    List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo,String value, String type);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    List<NetworkInfo> getNetwork(CredentialInfo credentialInfo, String networkId,Boolean webCheck);

    Object deleteNetwork(CredentialInfo credentialInfo, String NetworkId, Boolean webCheck);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);

    Object createNetwork(CredentialInfo credentialInfo, Map<String, Object> data, Boolean webCheck);

    List<FlavorInfo> getFlavors(CredentialInfo credentialInfo, String osType);

    List<ImageInfo> getImages(CredentialInfo credentialInfo);
}
