package com.datahub.infra.apitoast.service;

import com.datahub.infra.coretoast.model.*;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public interface ToastService {

    boolean validateCredential(CredentialInfo credentialInfo);

    List<Servers> getServers(CredentialInfo credentialInfo);

    ServerInfo getServer(CredentialInfo credentialInfo, String serverId);

    List<Volumes> getVolumes(CredentialInfo credentialInfo);

    VolumeInfo getVolume(CredentialInfo credentialInfo, String volumeId);

    TypeInfo getVolumeTypes(CredentialInfo credentialInfo);

    List<Snapshots> getSnapshots(CredentialInfo credentialInfo);

    SnapshotInfo getSnapshot(CredentialInfo credentialInfo, String snapshotId);

    Header deleteServer(CredentialInfo credentialInfo, String serverId);

    Header deleteVolume(CredentialInfo credentialInfo, String volumeId);

    Header deleteSnapshot(CredentialInfo credentialInfo, String snapshotId);

    /////////////////////////

    List<Subnets> getSubnets(CredentialInfo credentialInfo);

    List<SecurityGroups> getSecurityGroups(CredentialInfo credentialInfo);

    SecurityGroupInfo getSecurityGroups(CredentialInfo credentialInfo, String securityId);

    Header deleteSecurityGroup(CredentialInfo credentialInfo, String securityId);

    List<Floatingips> getFloatingips(CredentialInfo credentialInfo);

    FloatingipInfo getFloatingip(CredentialInfo credentialInfo, String floatingIpId);

    Header deleteFloatingip(CredentialInfo credentialInfo, String floatingIpId);

    List<Networks> getNetworks(CredentialInfo credentialInfo);

    ResourceInfo getResourceUsage(CredentialInfo credentialInfo);

    /////////////////////////

    List<ImageDetailInfo> getImageDetails(CredentialInfo credentialInfo, String type, String location);

    TokensInfo getToken(CredentialInfo credentialInfo);

    /////////////////////////

    VolumeInfo createVolume(CredentialInfo credentialInfo, Map<String, Object> createData);

    ServerInfo createServers(CredentialInfo credentialInfo, JSONObject createData);

    String serverAction(CredentialInfo credentialInfo, JSONObject actionData, String instanceId);

    List<ImageDetailInfo> getImageDetails(CredentialInfo credentialInfo);

    ImageDetailInfo getImagesDetails(CredentialInfo credentialInfo, String imageId);

    Header deleteImage(CredentialInfo credentialInfo, String imageId);

    List<ZoneDetailInfo> getZoneDetails(CredentialInfo credentialInfo);

    List<FlavorDetailInfo> getFlavorDetails(CredentialInfo credentialInfo);

    List<Keypair> getKeypairDetails(CredentialInfo credentialInfo);

    KeypairsDetailInfo postKeypair(CredentialInfo credentialInfo, String keypairData);

    KeypairsDetailInfo getKeypairsDetails(CredentialInfo credentialInfo, String keypairName);

    Header deleteKeypair(CredentialInfo credentialInfo, String keypairName);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);
}