package com.datahub.infra.apiopenstack.service;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coreopenstack.model.*;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.HypervisorStatistics;
import org.openstack4j.model.storage.block.VolumeType;

import java.net.MalformedURLException;
import java.util.List;

public interface OpenStackService {
    boolean validateCredential(CredentialInfo credentialInfo);

    List<ServerInfo> getServers(CredentialInfo credentialInfo, String projectId, Boolean webCheck);

    List<ServerInfo> getServers_model(CredentialInfo credentialInfo, String projectId, Boolean webCheck, Object serverModel);

    List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String projectId, String value, String type);

    List<ServerInfo> getServer(CredentialInfo credentialInfo, String projectId, String serverId, Boolean webCheck);

    Object createServer(CredentialInfo credentialInfo, String projectId, CreateServerInfo createServerInfo,Boolean webCheck);

    List<FlavorInfo> getFlavors(CredentialInfo credentialInfo, String projectId);

    List<ImageInfo> getImages(CredentialInfo credentialInfo, String projectId, Boolean active, Object imageModel);

    List<VolumeInfo> getVolumes(CredentialInfo credentialInfo, String projectId, Boolean webCheck);

    List<VolumeInfo> getVolumes(CredentialInfo credentialInfo, String projectId, Boolean bootable, Boolean available, Boolean webCheck);

    List<VolumeInfo> getVolumes_Search(CredentialInfo credentialInfo, String projectId, Boolean bootable, Boolean available, String value, String type);

    List<VolumeInfo> getVolume(CredentialInfo credentialInfo, String projectId, String volumeId, Boolean webCheck);

    Object createVolume(CredentialInfo credentialInfo, String projectId, CreateVolumeInfo createVolumeInfo, Boolean webCheck);

    DeleteInfo deleteVolume(CredentialInfo credentialInfo, String projectId, String volumeId);

    List<? extends VolumeType> getVolumeTypes(CredentialInfo credentialInfo);

    List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, String projectId, Boolean webCheck);

    List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String projectId, String value, String type);

    List<NetworkInfo> getNetwork(CredentialInfo credentialInfo, String projectId, String networkId, Boolean webCheck);

    Object createNetwork(CredentialInfo credentialInfo, String projectId, CreateNetworkInfo createNetworkInfo, Boolean webCheck);

    DeleteInfo deleteNetwork(CredentialInfo credentialInfo, String projectId, String networkId);

    DeleteInfo delete(CredentialInfo credentialInfo, String projectId, String serverId);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

    ProjectInfo getProject(CredentialInfo credentialInfo, String projectId);

    List<ProjectInfo> getProjects(CredentialInfo credentialInfo);

    List<ProjectInfo> getProjectsInMemory(CredentialInfo credentialInfo);

    String getProjectName(CredentialInfo credentialInfo, String projectId);

    List<NovaImageInfo> getAllImages(CredentialInfo credentialInfo, String projectId);

    void deleteCredential(CredentialInfo info, String projectId, String credentialId, CredentialDao credentialDao);
}
