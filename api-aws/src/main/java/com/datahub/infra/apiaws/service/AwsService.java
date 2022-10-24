package com.datahub.infra.apiaws.service;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coreaws.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import software.amazon.awssdk.services.ec2.model.Image;

import java.util.List;
import java.util.Map;

public interface AwsService {
    boolean validateCredential(CredentialInfo credentialInfo);

    List<ServerInfo> getServers(CredentialInfo credentialInfo, Boolean webCheck);

    List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String value, String type);

    List<ServerInfo> getServer(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    Object createServer(CredentialInfo credentialInfo, CreateServerInfo createServerInfo, Boolean webCheck);

    ServerInfo start(CredentialInfo credentialInfo, String serverId);

    ServerInfo stop(CredentialInfo credentialInfo, String serverId);

    ServerInfo reboot(CredentialInfo credentialInfo, String serverId);

    Object delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    ServerInfo monitoring(CredentialInfo credentialInfo, String serverId);

    ServerInfo unmonitoring(CredentialInfo credentialInfo, String serverId);

    Object createVolume(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    List<RegionInfo> getRegions(CredentialInfo credentialInfo);

    List<ZoneInfo> getAvailabilityZones(CredentialInfo credentialInfo);

    List<VolumeInfo> getVolumes(CredentialInfo credentialInfo,Boolean webCheck);

    List<VolumeInfo> getVolumes_Search(CredentialInfo credentialInfo, String value, String type);

    List<VolumeInfo> getVolume(CredentialInfo credentialInfo, String volumeId,Boolean webCheck);

    Object deleteVolume(CredentialInfo credentialInfo, String volumeId, Boolean webCheck);

    List<SnapshotInfo> getSnapshots(CredentialInfo credentialInfo);

    List<ImageInfo> getImages(CredentialInfo credentialInfo);

    List<com.datahub.infra.coreaws.model.KeyPairInfo> getKeyPairs(CredentialInfo credentialInfo);

    List<NetworkInfo> getNetworks(CredentialInfo credentialInfo,Boolean webCheck);

    List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo,String value, String type);

    List<SecurityGroupInfo> getSecurityGroups(CredentialInfo credentialInfo);

    List<AddressInfo> getAddresses(CredentialInfo credentialInfo);

    List<SubnetInfo> getSubnets(CredentialInfo credentialInfo,  String vpcId);

    List<VpcInfo> getVpcs(CredentialInfo credentialInfo);

    List<UserInfo> getUsers(CredentialInfo credentialInfo);

    List<GroupInfo> getGroups(CredentialInfo credentialInfo);

    Map<String, Object> getServerMetric(CredentialInfo credentialInfo, RequestMetricInfo requestMetricInfo);

    List<GroupInfo> imageTest(CredentialInfo credentialInfo);

    List<GroupInfo> resourceTest(CredentialInfo credentialInfo);

    List<GroupInfo> s3Test(CredentialInfo credentialInfo);

    ResourceInfo getResourceUsage(CredentialInfo credentialInfo);

    Image getImageDetail(CredentialInfo credentialInfo, String imageId);

    List<FlavorInfo> getFlavors(CredentialInfo credentialInfo, String osType);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    List<NetworkInfo> getNetwork(CredentialInfo credentialInfo, String networkId,Boolean webCheck);

    Object deleteNetwork(CredentialInfo credentialInfo, String NetworkId, Boolean webCheck);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);

    Object createNetwork(CredentialInfo credentialInfo, Map<String, Object> data, Boolean webCheck);


}
