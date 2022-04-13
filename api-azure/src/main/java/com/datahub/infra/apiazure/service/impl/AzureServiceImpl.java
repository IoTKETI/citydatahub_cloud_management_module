package com.datahub.infra.apiazure.service.impl;

import com.datahub.infra.apiazure.service.AzureService;
import com.datahub.infra.core.exception.CityHubUnAuthorizedException;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coreazure.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.rest.LogLevel;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@Service
public class AzureServiceImpl implements AzureService {
    private final static Logger logger = LoggerFactory.getLogger(AzureServiceImpl.class);

    private String subscriptionId;
    private String subscriptionDisplayName;

    private Azure Credential(CredentialInfo credentialInfo) {

        Azure azure = null;
        try {
            ApplicationTokenCredentials credential = new ApplicationTokenCredentials(
                    credentialInfo.getAccessId(),
                    credentialInfo.getTenant(),
                    credentialInfo.getAccessToken(),
                    AzureEnvironment.AZURE
            );

            Azure.Authenticated authenticated = Azure.configure()
                    .withLogLevel(LogLevel.BASIC)
                    .authenticate(credential);

            if(credentialInfo.getProjectId()==null ||credentialInfo.getProjectId().isEmpty()){
                azure = authenticated.withDefaultSubscription();
            }else{
                azure = authenticated.withSubscription(credentialInfo.getSubscriptionId());
            }
            this.subscriptionDisplayName = azure.subscriptions().getById(azure.subscriptionId()).displayName();
            this.subscriptionId = azure.subscriptionId();

            return azure;
        } catch(Exception e) {
            logger.error("Failed to get azure credential : '{}'", e.getMessage());
            e.printStackTrace();
            throw new CityHubUnAuthorizedException(e.getMessage());
        }
    }

    public boolean validateCredential(CredentialInfo info) {
        boolean isValid = true;

        Azure azure = null;
        ApplicationTokenCredentials credential = new ApplicationTokenCredentials(
                info.getAccessId(),
                info.getTenant(),
                info.getAccessToken(),
                AzureEnvironment.AZURE
        );
        try {
            Azure.Authenticated authenticated = Azure.configure()
                    .withLogLevel(LogLevel.BASIC)
                    .authenticate(credential);
            azure = authenticated.withDefaultSubscription();
        } catch (Exception e) {
            isValid = false;
            logger.error("Failed to validate credential : '{}'", e.getMessage());
        }

        return isValid;
    }

    public List<ServerInfo> getServers(CredentialInfo credentialInfo,Boolean webCheck) {

        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        Azure azure = Credential(credentialInfo);
        PagedList<VirtualMachine> vms = azure.virtualMachines().list();

        if(webCheck) {
            for (VirtualMachine vm : vms) {
                ServerInfo info = new ServerInfo(vm);
                info.setSubscriptionId(this.subscriptionId);
                info.setSubscriptionDisplayName(this.subscriptionDisplayName);
                list.add(info);
            }
        }
        else{
            for (VirtualMachine vm : vms) {
                list2.add(getServerInfo2(vms, vm, azure));
            }
        }

        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public List<ServerInfo> getServers_Search(CredentialInfo credentialInfo, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<ServerInfo2> list2 = new ArrayList<>();
        Azure azure = Credential(credentialInfo);
        PagedList<VirtualMachine> vms = azure.virtualMachines().list();

        for (VirtualMachine vm : vms) {
            if(type.equals("name")){
                if(vm.name().equals(value)) list2.add(getServerInfo2(vms, vm, azure));
            }
            if(type.equals("serverState")){
                String state = vm.powerState().toString().split("/")[1];
                if(state.equals(value)) list2.add(getServerInfo2(vms, vm, azure));
            }
        }

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }

    public List<ServerInfo> getServer(CredentialInfo credentialInfo, String serverId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();
        Azure azure = Credential(credentialInfo);
        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        ServerInfo serverInfo;

        PagedList<VirtualMachine> vms = azure.virtualMachines().list();
        VirtualMachine vm = azure.virtualMachines().getById(serverId);

        if(webCheck){
            if(vm != null) {
                serverInfo = new ServerInfo(vm);
                list.add(serverInfo);
            }
        }
        else{
            if(vm != null) {
                list2.add(getServerInfo2(vms, vm, azure));
            }
        }

        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public List<ServerInfo> getServer_detail(CredentialInfo credentialInfo, String moduleName, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();

        PagedList<VirtualMachine> vms = azure.virtualMachines().list();
        VirtualMachine vm = azure.virtualMachines().getById(moduleName);

        if(webCheck) {
            ServerInfo serverInfo = new ServerInfo(vm);
            serverInfo.setSubscriptionId(this.subscriptionId);
            serverInfo.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(serverInfo);
        }
        else {
            list2.add(getServerInfo2(vms, vm, azure));
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public DeleteInfo deleteServer_test(CredentialInfo credentialInfo, String moduleName) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        VirtualMachine vm = azure.virtualMachines().getById(moduleName);
        ServerInfo serverInfo = new ServerInfo(vm);
        serverInfo.setSubscriptionId(subscriptionId);
        serverInfo.setSubscriptionDisplayName(subscriptionDisplayName);

        DeleteInfo deleteinfo = new DeleteInfo();

        try {
            deleteinfo.setId(serverInfo.getId());
            deleteinfo.setName(serverInfo.getName());
            azure.virtualMachines().deleteById(moduleName);
            return deleteinfo;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ServerInfo> getServers(CredentialInfo credentialInfo, String serverId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<ServerInfo> list = new ArrayList<>();
        List<ServerInfo2> list2 = new ArrayList<>();
        Azure azure = Credential(credentialInfo);
        PagedList<VirtualMachine> vms = azure.virtualMachines().list();

        if(webCheck) {
            for (VirtualMachine vm : vms) {
                ServerInfo info = new ServerInfo(vm);
                info.setSubscriptionId(this.subscriptionId);
                info.setSubscriptionDisplayName(this.subscriptionDisplayName);
                list.add(info);
            }
        }
        else{
            for (VirtualMachine vm : vms) {
                list2.add(getServerInfo2(vms, vm, azure));
            }
        }

        List<ServerInfo2> list3 = new ArrayList<>();
        if(!webCheck){
            for(ServerInfo2 temp : list2){
                if(temp.getId().equals('/'+serverId)){
                    list3.add(temp);
                }
            }
        }

        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list3);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public List<DiskInfo> getDisks(CredentialInfo credentialInfo, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Azure azure = Credential(credentialInfo);
        PagedList<Disk> disks = azure.disks().list();
        List<DiskInfo> list = new ArrayList<>();
        List<DiskInfo2> list2 = new ArrayList<>();
        if(webCheck) {
            for (Disk disk : disks) {
                DiskInfo info = new DiskInfo(disk);
                info.setSubscriptionId(this.subscriptionId);
                info.setSubscriptionDisplayName(this.subscriptionDisplayName);
                list.add(info);
            }
        }
        else{
            for (Disk disk : disks) {
                DiskInfo2 info = new DiskInfo2(disk);
                list2.add(info);
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public List<DiskInfo> getDisks_Search(CredentialInfo credentialInfo, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Azure azure = Credential(credentialInfo);
        PagedList<Disk> disks = azure.disks().list();
        List<DiskInfo> list = new ArrayList<>();
        List<DiskInfo2> list2 = new ArrayList<>();

        for (Disk disk : disks) {
            DiskInfo2 info = new DiskInfo2(disk);
            if(type.equals("name")){
                if(info.getName().equals(value)) list2.add(info);
            }
            if(type.equals("volumeState")){
                if(info.getIsAttached().toString().equals(value)) list2.add(info);
            }
        }

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }

    public List<DiskInfo> getDisk(CredentialInfo credentialInfo, String moduleName, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        List<DiskInfo> list = new ArrayList<>();
        List<DiskInfo2> list2 = new ArrayList<>();


        Azure azure = Credential(credentialInfo);
        Disk disk = azure.disks().getById(moduleName);
        if(webCheck) {

            DiskInfo diskInfo = new DiskInfo(disk);
            diskInfo.setSubscriptionId(this.subscriptionId);
            diskInfo.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(diskInfo);
        }
        else{
            DiskInfo2 diskInfo = new DiskInfo2(disk);
            list2.add(diskInfo);
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public DiskInfo deleteDisk_test(CredentialInfo credentialInfo, String moduleName) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        Disk disk = azure.disks().getById(moduleName);

        DiskInfo diskInfo = new DiskInfo(disk);
        diskInfo.setSubscriptionId(this.subscriptionId);
        diskInfo.setSubscriptionDisplayName(this.subscriptionDisplayName);

        azure.disks().deleteById(moduleName);

        return diskInfo;
    }

    public List<NetworkInfo> getNetworks(CredentialInfo credentialInfo, String resourceGroup, String region,Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        region = region != null ? region : null;
        resourceGroup = resourceGroup != null ? resourceGroup : null;

        Azure azure = Credential(credentialInfo);

        PagedList<Network> networks = azure.networks().list();
        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        if(webCheck) {
            for (Network network : networks) {

                if (region != null && !network.region().toString().equals(region)) continue;
                if (resourceGroup != null && !network.resourceGroupName().equals(resourceGroup)) continue;

                NetworkInfo info = new NetworkInfo(network);
                info.setSubscriptionId(this.subscriptionId);
                info.setSubscriptionDisplayName(this.subscriptionDisplayName);
                list.add(info);
            }
        }
        else{
            for (Network network : networks) {

                if (region != null && !network.region().toString().equals(region)) continue;
                if (resourceGroup != null && !network.resourceGroupName().equals(resourceGroup)) continue;

                NetworkInfo2 info2 = new NetworkInfo2(network);
                list2.add(info2);
            }
        }
        try {
            jsonString = mapper.writeValueAsString(list);
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
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

    public List<NetworkInfo> getNetworks_Search(CredentialInfo credentialInfo, String resourceGroup, String region, String value, String type) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        region = region != null ? region : null;
        resourceGroup = resourceGroup != null ? resourceGroup : null;

        Azure azure = Credential(credentialInfo);

        PagedList<Network> networks = azure.networks().list();
        List<NetworkInfo2> list2 = new ArrayList<>();

        for (Network network : networks) {
            if (region != null && !network.region().toString().equals(region)) continue;
            if (resourceGroup != null && !network.resourceGroupName().equals(resourceGroup)) continue;

            NetworkInfo2 info2 = new NetworkInfo2(network);
            if(type.equals("name")){
                if (info2.getName().equals(value)) list2.add(info2);
            }

            if(type.equals("networkState")){
                if (info2.getState().equals(value)) list2.add(info2);
            }

        }

        try {
            jsonString2 = mapper.writeValueAsString(list2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        return jsonArray2;
    }

    public List<NetworkInfo> getNetworks_Detail_azure(CredentialInfo credentialInfo, String resourceGroup, String region, String subscriptions, HttpServletRequest request, String moduleName,Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        region = region != null ? region : null;
        resourceGroup = resourceGroup != null ? resourceGroup : null;

        Azure azure = Credential(credentialInfo);

        PagedList<Network> networks = azure.networks().list();
        List<NetworkInfo> list = new ArrayList<>();
        List<NetworkInfo2> list2 = new ArrayList<>();
        for (Network network : networks) {
            if (region != null && !network.region().toString().equals(region)) continue;
            if (resourceGroup != null && !network.resourceGroupName().equals(resourceGroup)) continue;

            NetworkInfo info = new NetworkInfo(network);
            NetworkInfo2 info2 = new NetworkInfo2(network);
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);


            String moduleName2 = "/" + moduleName;

            if (webCheck) {
                for (int i = 0; i < info.getId().length(); i++) {

                    if (info.getId() != null && info.getId().equals(moduleName2)) {

                        list.add(info);
                        break;
                    } else {

                    }
                }
            }
            else{
                for (int i = 0; i < info2.getId().length(); i++) {

                    if (info2.getId() != null && info.getId().equals(moduleName2)) {
                        list2.add(info2);
                        break;
                    } else {
                    }
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
        } catch(IOException e) {
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

    public Object getNetworks_Create_test(CredentialInfo credentialInfo, @RequestBody Map<String, Object> createData, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String jsonString = null;
        String jsonString2 = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        String network_name = null;
        String group_name = null;
        String prefix_name = null;
        String region = null;

        if(webCheck){
            network_name = createData.get("network_name").toString();
            group_name = createData.get("group_name").toString();
            prefix_name = createData.get("prefix_name").toString();
            region = createData.get("region").toString();
        }else{
            network_name = createData.get("name").toString();
            group_name = createData.get("resourceGroupName").toString();
            prefix_name = createData.get("ip").toString();
            region = createData.get("region").toString();

        }

        Azure azure = Credential(credentialInfo);

        Network virtualNetwork1 = azure.networks().define(network_name)
                .withRegion(region)
                .withExistingResourceGroup(group_name)
                .withAddressSpace(prefix_name)
                .defineSubnet(network_name)
                .withAddressPrefix(prefix_name)
                .attach()
                .create();

        List<NetworkInfo> info = new ArrayList<>();
        List<NetworkInfo2> info2 = new ArrayList<>();

        if(webCheck){
            NetworkInfo temp = new NetworkInfo(virtualNetwork1);
            info.add(temp);
        }else{
            NetworkInfo2 temp = new NetworkInfo2(virtualNetwork1);
            info2.add(temp);
        }
        try {
            jsonString = mapper.writeValueAsString(info);
            jsonString2 = mapper.writeValueAsString(info2);
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

    public DeleteInfo deleteNetwork(CredentialInfo credentialInfo, String moduleName, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        try {
            Network network = azure.networks().getById(moduleName);
            NetworkInfo networkInfo = new NetworkInfo(network);
            networkInfo.setSubscriptionId(this.subscriptionId);
            networkInfo.setSubscriptionDisplayName(this.subscriptionDisplayName);

            DeleteInfo deleteinfo = new DeleteInfo();
            deleteinfo.setId(networkInfo.getId());
            deleteinfo.setName(networkInfo.getName());
            azure.networks().deleteById(moduleName);
            return deleteinfo;

        } catch(Exception e) {
            if(!webCheck) { moduleName = java.util.Base64.getEncoder().encodeToString(moduleName.getBytes()); }
            throw new NullPointerException("Instance " + moduleName + " could not be found.");
        }
    }

    public DeleteInfo delete(CredentialInfo credentialInfo, String serverId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        try {
            VirtualMachine virtualMachine = azure.virtualMachines().getById(serverId);
            ServerInfo serverInfo = new ServerInfo(virtualMachine);
            serverInfo.setSubscriptionId(subscriptionId);
            serverInfo.setSubscriptionDisplayName(subscriptionDisplayName);

            DeleteInfo deleteinfo = new DeleteInfo();

            deleteinfo.setId(serverInfo.getId());
            deleteinfo.setName(serverInfo.getName());
            azure.virtualMachines().deleteById(serverId);
            return deleteinfo;

        } catch(Exception e) {
            if(!webCheck) { serverId = java.util.Base64.getEncoder().encodeToString(serverId.getBytes()); }
            throw new NullPointerException("Instance " + serverId + " could not be found.");
        }
    }

    public Object createServer(CredentialInfo credentialInfo, Map<String, Object> createData, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        String name = createData.get("name").toString();
        String region = createData.get("region").toString();


        String username;
        String password;
        String resourceGroupType;
        String size;
        String imageType;
        String networkType;
        String publicIpType;
        String publicIp ;

        String inboundPortType;
        List<String> inboundPort;

        String script = createData.get("script") == null ? null : createData.get("script").toString();

        if(webCheck){
            username = createData.get("username").toString();
            password = createData.get("password").toString();
            resourceGroupType = createData.get("resourceGroupType").toString();
            size = createData.get("size").toString();
            imageType = createData.get("imageType").toString();
            networkType = createData.get("networkType").toString();
            publicIpType = createData.get("publicIpType").toString();
            publicIp = createData.get("publicIp").toString();
            inboundPortType = createData.get("inboundPortType").toString();
            inboundPort = (List<String>) createData.get("inboundPort");
        }else{
            username = "createTest";
            password = "qwe1212!Q";
            resourceGroupType = "exist";
            size = createData.get("flavorName").toString();
            imageType = createData.get("imageType").toString();
            networkType = "exist";
            publicIpType = createData.get("publicIpType").toString();
            if(publicIpType.equals("none")){
                publicIp = "";
            }else{
                publicIp = createData.get("publicIp").toString();
            }
            inboundPortType = "none";
            inboundPort = null;
        }

        String resourceGroupName = createData.get("resourceGroupName").toString();

        String imageOS = createData.get("osType").toString();
        String imageId = createData.get("imageId").toString();

        String subnet = createData.get("subnetName").toString();

        String network = createData.get("networkId").toString();

        VirtualMachine.DefinitionStages.Blank regionStage = azure.virtualMachines().define(name);

        VirtualMachine.DefinitionStages.WithGroup resourceGroupStage = regionStage.withRegion(region);

        VirtualMachine.DefinitionStages.WithNetwork networkStage = null;
        if (resourceGroupType.equals("exist")) {
            networkStage = resourceGroupStage.withExistingResourceGroup(resourceGroupName);
        } else if (resourceGroupType.equals("new")) {
            networkStage = resourceGroupStage.withNewResourceGroup(resourceGroupName);
        } else {
            networkStage = resourceGroupStage.withNewResourceGroup();
        }

        VirtualMachine.DefinitionStages.WithPrivateIP privateIpStage = null;
        if (networkType.equals("exist")) {
            VirtualMachine.DefinitionStages.WithSubnet subnetStage = networkStage.withExistingPrimaryNetwork(azure.networks().getById(network));
            privateIpStage = subnetStage.withSubnet(subnet);
        } else if(networkType.equals("new")) {
            privateIpStage = networkStage.withNewPrimaryNetwork(network);
        }

        VirtualMachine.DefinitionStages.WithPublicIPAddress publicIpStage = privateIpStage.withPrimaryPrivateIPAddressDynamic();

        VirtualMachine.DefinitionStages.WithOS imageStage = null;
        if (publicIpType.equals("exist")) {
            imageStage = publicIpStage.withExistingPrimaryPublicIPAddress(azure.publicIPAddresses().getById(publicIp));
        } else if (publicIpType.equals("new")) {
            imageStage = publicIpStage.withNewPrimaryPublicIPAddress(publicIp);
        } else {
            imageStage = publicIpStage.withoutPrimaryPublicIPAddress();
        }

        VirtualMachine.DefinitionStages.WithLinuxCreateManaged linuxCreateStage = null;
        VirtualMachine.DefinitionStages.WithWindowsCreateManaged windowsCreate = null;
        VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged publicLinuxCreateStage = null;
        VirtualMachine.DefinitionStages.WithWindowsCreateManagedOrUnmanaged publicWindowsCreate = null;

        if(imageType.equals("custom")) {
            if (imageOS.equals("Linux")) {
                VirtualMachine.DefinitionStages.WithLinuxRootUsernameManaged usernameStage = imageStage.withLinuxCustomImage(imageId);
                VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManaged passwordStage = usernameStage.withRootUsername(username);
                linuxCreateStage = passwordStage.withRootPassword(password);
            } else {
                VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManaged usernameStage = imageStage.withWindowsCustomImage(imageId);
                VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManaged passwordStage = usernameStage.withAdminUsername(username);
                windowsCreate = passwordStage.withAdminPassword(password);
            }
        } else if(imageType.equals("public")) {

            String[] urnArr = imageId.split(":");
            String publisher = urnArr[1];
            String offer = urnArr[2];
            String sku = urnArr[3];
            String version = urnArr[4];

            ImageReference imageReference = new ImageReference();
            imageReference.withPublisher(publisher).withOffer(offer).withSku(sku).withVersion(version);

            if(imageOS.equals("Linux")) {
                VirtualMachine.DefinitionStages.WithLinuxRootUsernameManagedOrUnmanaged usernameStage = imageStage.withSpecificLinuxImageVersion(imageReference);
                VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged passwordStage = usernameStage.withRootUsername(username);
                publicLinuxCreateStage = passwordStage.withRootPassword(password);
            } else {
                VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManagedOrUnmanaged usernameStage = imageStage.withSpecificWindowsImageVersion(imageReference);
                VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManagedOrUnmanaged passwordStage = usernameStage.withAdminUsername(username);
                publicWindowsCreate = passwordStage.withAdminPassword(password);
            }
        }

        VirtualMachine server = null;

        if(imageType.equals("custom")) {
            if (linuxCreateStage != null) {
                VirtualMachine.DefinitionStages.WithCreate sizeStage = linuxCreateStage.withSize(size);
                server = sizeStage.create();
            } else {
                server = windowsCreate.withSize(size).create();
            }
        } else if(imageType.equals("public")) {
            if(publicLinuxCreateStage != null) {
                server = publicLinuxCreateStage.withSize(size).create();
            } else {
                server = publicWindowsCreate.withSize(size).create();
            }
        }

        if(inboundPortType.equals("allow")) {

            NetworkSecurityGroup networkSecurityGroup = azure.networkSecurityGroups().define(name+"-NSG")
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroupName)
                    .create();

            for(int i=0; i<inboundPort.size(); i++) {
                String portName = inboundPort.get(i);
                int port = 0;

                if (portName.equals("HTTP")) {
                    port = 80;
                } else if (portName.equals("HTTPS")) {
                    port = 443;
                } else if (portName.equals("SSH")) {
                    port = 22;
                } else if (portName.equals("RDP")) {
                    port = 3389;
                }

                networkSecurityGroup.update()
                        .defineRule("ALLOW-"+portName)
                        .allowInbound()
                        .fromAnyAddress()
                        .fromAnyPort()
                        .toAnyAddress()
                        .toPort(port)
                        .withProtocol(SecurityRuleProtocol.TCP)
                        .withPriority(300 + i*20) // 기본 priority는 300
                        .attach()
                        .apply();
            }

            NetworkInterface networkInterface = server.getPrimaryNetworkInterface();
            networkInterface.update()
                    .withExistingNetworkSecurityGroup(networkSecurityGroup);
        }

        if(script != null && !script.isEmpty()) {
            List<String> commandList = new ArrayList<>();
            String[] commands = script.split("\\r?\\n");
            for(String command : commands) {
                if(command.length() == 0) continue;
                commandList.add(command);
            }
            RunCommandResult runCommandResult = runCommandOnVm(credentialInfo, server, commandList);
            List<InstanceViewStatus> instanceViewStatusList = runCommandResult.value();
            for(InstanceViewStatus instanceViewStatus : instanceViewStatusList) {
                System.out.println(instanceViewStatus.message());
            }
        }

        String jsonString = null;
        String jsonString2 = null;

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<ServerInfo> info = new ArrayList<>();
        List<ServerInfo2> info2 = new ArrayList<>();

        PagedList<VirtualMachine> vms = azure.virtualMachines().list();

        if(webCheck){
            ServerInfo serverInfo = new ServerInfo(server);
            info.add(serverInfo);
        }else{
            info2.add(getServerInfo2(vms, server, azure));
        }

        try {
            jsonString = mapper.writeValueAsString(info);
            jsonString2 = mapper.writeValueAsString(info2);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);

        if (webCheck) {
            return jsonArray;
        }
        else {
            return null;
        }
    }

    public Object createDisk(CredentialInfo credentialInfo, @RequestBody Map<String, Object> createData, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        String disk_name;
        String region;
        String group_name;
        String disk_size;
        if(webCheck){
            disk_name = createData.get("disk_name").toString();
            region = createData.get("region").toString();
            group_name = createData.get("group_name").toString();
            disk_size = createData.get("disk_size").toString();
        }else{
            disk_name = createData.get("name").toString();
            region = createData.get("region").toString();
            group_name = createData.get("securityGroupName").toString();
            disk_size = createData.get("size").toString();
        }

        String jsonString = null;
        String jsonString2 = null;

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Azure azure = Credential(credentialInfo);

        Disk osDisk = azure.disks().define(disk_name)
                .withRegion(region)
                .withExistingResourceGroup(group_name)
                .withData()
                .withSizeInGB(Integer.parseInt(disk_size))
                .create();

        List<DiskInfo> info = new ArrayList<>();
        List<DiskInfo2> info2 = new ArrayList<>();

        if(webCheck){
            DiskInfo temp = new DiskInfo(osDisk);
            info.add(temp);
        }else{
            DiskInfo2 temp = new DiskInfo2(osDisk);
            info2.add(temp);
        }

        try {
            jsonString = mapper.writeValueAsString(info);
            jsonString2 = mapper.writeValueAsString(info2);
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

    public DeleteInfo deleteDisk(CredentialInfo credentialInfo, String volumeId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        try {
            Disk diskInfo = azure.disks().getById(volumeId);

            DeleteInfo deleteinfo = new DeleteInfo();

            deleteinfo.setId(diskInfo.id());
            deleteinfo.setName(diskInfo.name());
            azure.disks().deleteById(volumeId);

            if(webCheck){
                return deleteinfo;
            }else{
                return null;
            }


        } catch(Exception e) {
            if(!webCheck) { volumeId = java.util.Base64.getEncoder().encodeToString(volumeId.getBytes()); }
            throw new NullPointerException("Instance " + volumeId + " could not be found.");
        }
    }

    @Override
    public List<CredentialInfo> getCredential(List<CredentialInfo> list, String type) {

        List <CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo info = list.get(i);
            if(info.getType().equals(type)){
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

    public ServerInfo2 getServerInfo2(PagedList<VirtualMachine> vms, VirtualMachine vm, Azure azure) {

        ServerInfo2 info;

        String region = "koreacentral";
        Region regionInfo = Region.fromName(region);

        PagedList<Network> networks = azure.networks().list();
        PagedList<VirtualMachineCustomImage> images = azure.virtualMachineCustomImages().list();
        PagedList<PublicIPAddress> publicIPAddresses = azure.publicIPAddresses().list();
        PagedList<ComputeSku> skuList = azure.computeSkus().listbyRegionAndResourceType(regionInfo, ComputeResourceType.VIRTUALMACHINES);
        PagedList<Disk> disks = azure.disks().list();
        info = new ServerInfo2(vm, vms, networks, images, publicIPAddresses, skuList, disks);
        return info;
    }

    public RunCommandResult runCommandOnVm(CredentialInfo credentialInfo, VirtualMachine vm, List<String> scripts) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        String commandId = "";
        if(vm.osType().equals("Linux")) {
            commandId = "RunShellScript";
        } else if (vm.osType().equals("Windows")){
            commandId = "RunPowerShellScript";
        }

        RunCommandInput runCommandInput = new RunCommandInput()
                .withCommandId(commandId)
                .withScript(scripts);

        return azure.virtualMachines().runCommand(vm.resourceGroupName(), vm.name(), runCommandInput);
    }
}
