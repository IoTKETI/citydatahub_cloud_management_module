package com.datahub.infra.apiazure.service;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coreazure.model.DeleteInfo;
import com.datahub.infra.coreazure.model.DiskInfo;
import com.datahub.infra.coreazure.model.NetworkInfo;
import com.datahub.infra.coreazure.model.ServerInfo;
import com.datahub.infra.coredb.dao.CredentialDao;

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

    List<DiskInfo> getDisks(CredentialInfo credentialInfo,Boolean webCheck);

    List<DiskInfo> getDisks_Search(CredentialInfo credentialInfo, String value, String type);

    List<DiskInfo> getDisk(CredentialInfo credentialInfo, String moduleName,Boolean webCheck);

    DiskInfo deleteDisk_test(CredentialInfo credentialInfo, String moduleName);

    List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, String resourceGroup, String Region,Boolean webCheck);

    List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String resourceGroup, String Region, String value, String type);

    List<NetworkInfo> getNetworks_Detail_azure(CredentialInfo credentialInfo, String resourceGroup, String Region, String subscriptions, HttpServletRequest request, String moduleName,Boolean webCheck);

    DeleteInfo deleteNetwork(CredentialInfo credentialInfo, String moduleName, Boolean webCheck);

    Object getNetworks_Create_test(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    DeleteInfo delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck);

    Object createServer(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    Object createDisk(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);

    DeleteInfo deleteDisk(CredentialInfo credentialInfo, String volumeId, Boolean webCheck);
}
