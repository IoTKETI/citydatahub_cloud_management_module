package com.datahub.infra.apiazure.service;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.model.ImageDetailInfo;
import com.datahub.infra.coreazure.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.microsoft.azure.management.compute.VirtualMachineImage;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


public interface AzureService {

    boolean validateCredential(CredentialInfo credentialInfo);

    List<ServerInfo> getServers(CredentialInfo credentialInfo, Boolean webCheck);

    List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String value, String type);

    List<ServerInfo> getServers(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    List<ServerInfo> getServer(CredentialInfo credentialInfo, String cloudId, Boolean webCheck);

    List<ServerInfo>  getServer_detail(CredentialInfo credentialInfo, String moduleName,Boolean webCheck);

    DeleteInfo deleteServer_test(CredentialInfo credentialInfo, String moduleName);

    List<ImageInfo> getImages(CredentialInfo credentialInfo);

    List<DiskInfo> getDisks(CredentialInfo credentialInfo,Boolean webCheck);

    List<DiskInfo> getDisks_Search(CredentialInfo credentialInfo, String value, String type);

    List<DiskInfo> getDisk(CredentialInfo credentialInfo, String moduleName,Boolean webCheck);

    DiskInfo deleteDisk_test(CredentialInfo credentialInfo, String moduleName);

    List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, String resourceGroup, String Region,Boolean webCheck);

    List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String resourceGroup, String Region, String value, String type);

    List<NetworkInfo> getNetworks_Detail_azure(CredentialInfo credentialInfo, String resourceGroup, String Region, String subscriptions, HttpServletRequest request, String moduleName,Boolean webCheck);

    DeleteInfo deleteNetwork(CredentialInfo credentialInfo, String moduleName, Boolean webCheck);

    Object getNetworks_Create_test(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    Map<String,String> getIsUsableIp(CredentialInfo credentialInfo, String network, String privateIp);

    List<PublicIpInfo> getPublicIps(CredentialInfo credentialInfo, String resourceGroup, String region);

    List<LoadBalancerInfo> getLoadBalancers(CredentialInfo credentialInfo);

    List<SecurityGroupInfo> getSecurityGroups(CredentialInfo credentialInfo);

    List<ActiveDirectoryGroupInfo> getActiveDirectoryGroups(CredentialInfo credentialInfo);

    List<ActiveDirectoryUserInfo> getActiveDirectoryUsers(CredentialInfo credentialInfo);

    List<StorageAccountInfo> getStorageAccounts(CredentialInfo credentialInfo);

    List<GenericResourceInfo> getGenericResources(CredentialInfo credentialInfo);

    Map<String, Integer> getGenericResourcesSummary(CredentialInfo credentialInfo);

    ServerInfo start(CredentialInfo credentialInfo, String serverId);

    ServerInfo stop(CredentialInfo credentialInfo, String serverId);

    ServerInfo reboot(CredentialInfo credentialInfo, String serverId);

    DeleteInfo delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    Map<String, Object> getServerMetric(CredentialInfo credentialInfo, RequestMetricInfo requestMetricInfo);

    List<ResourceGroupInfo> getResourceGroups(CredentialInfo credentialInfo, String region);

    List<SizeInfo> getSizes(CredentialInfo credentialInfo, String region);

    Map<String, Map> getDashboard(CredentialInfo credentialInfo);

    Object createServer(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    Object createDisk(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    ResourceInfo getResourceUsage(CredentialInfo credentialInfo);

    List<SubscriptionInfo> getSubscriptions(CredentialInfo credentialInfo);

    List<RegionInfo> getRegions(CredentialInfo credentialInfo);

    VirtualMachineImage getImageDetail(CredentialInfo credentialInfo, String region, String imageId);

    List<ImageDetailInfo> getPublicImages(CredentialInfo credentialInfo);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);

    DeleteInfo deleteDisk(CredentialInfo credentialInfo, String volumeId, Boolean webCheck);
}
