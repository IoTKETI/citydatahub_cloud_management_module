package com.datahub.infra.apiazure.service.impl;

import com.datahub.infra.apiazure.service.AzureService;
import com.datahub.infra.core.exception.CityHubUnAuthorizedException;
import com.datahub.infra.core.exception.CredentialException;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.model.ImageDetailInfo;
import com.datahub.infra.coreazure.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.management.graphrbac.ActiveDirectoryGroup;
import com.microsoft.azure.management.graphrbac.ActiveDirectoryUser;
import com.microsoft.azure.management.monitor.MetricCollection;
import com.microsoft.azure.management.monitor.MetricDefinition;
import com.microsoft.azure.management.monitor.MetricValue;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageUsage;
import com.microsoft.rest.LogLevel;
import net.sf.json.JSONArray;
import org.joda.time.DateTime;
import org.joda.time.Period;
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
                logger.error("azure111 = " + azure);
            }else{
                azure = authenticated.withSubscription(credentialInfo.getSubscriptionId());
                logger.error("azure222 = " + azure);
            }
            this.subscriptionDisplayName = azure.subscriptions().getById(azure.subscriptionId()).displayName();
            this.subscriptionId = azure.subscriptionId();


            return azure;
        } catch(Exception e) {
            logger.error("Failed to get azure credential : '{}'", e.getMessage());
            e.printStackTrace();
            throw new CityHubUnAuthorizedException(e.getMessage());
        }
//        return null;
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

    public List<ResourceGroupInfo> getResourceGroups(CredentialInfo credentialInfo, String region) {

        region = region != null ? region : null;

        Azure azure = Credential(credentialInfo);
        PagedList<ResourceGroup> resourceGroups = azure.resourceGroups().list();
        List<ResourceGroupInfo> list = new ArrayList<>();
        for (ResourceGroup resourceGroup : resourceGroups) {
            if(region != null && !resourceGroup.region().toString().equals(region)) continue;

            ResourceGroupInfo info = new ResourceGroupInfo(resourceGroup);
            List<GenericResourceInfo> genericResourceInfoList = new ArrayList<>();
            PagedList<GenericResource> genericResources = azure.genericResources().listByResourceGroup(resourceGroup.id());
            for(GenericResource genericResource : genericResources){
                GenericResourceInfo genericResourceInfo = new GenericResourceInfo(genericResource);
                genericResourceInfoList.add(genericResourceInfo);
            }
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            info.setResources(genericResourceInfoList);
            list.add(info);
        }
        return list;
    }

    public List<SizeInfo> getSizes(CredentialInfo credentialInfo, String region) {

        Azure azure = Credential(credentialInfo);

        List<SizeInfo> vmSizeList = new ArrayList<>();

        Region regionInfo = Region.fromName(region);
        PagedList<ComputeSku> skuList = azure.computeSkus().listbyRegionAndResourceType(regionInfo, ComputeResourceType.VIRTUALMACHINES);
        for(ComputeSku sku : skuList) {
            if (sku.resourceType().equals(ComputeResourceType.VIRTUALMACHINES)) {
                SizeInfo vmSizeInfo = new SizeInfo(sku);
                vmSizeList.add(vmSizeInfo);
            }
        }
        return  vmSizeList;
    }

    public List<ServerInfo> getServers(CredentialInfo credentialInfo,Boolean webCheck) {

        logger.error("AzureServiceImpl, credentialInfo, credentialInfo? = {}", credentialInfo);

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


        logger.error("AzureServiceImpl, getServers, webCheck? = {}", webCheck);

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
//        return list;
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
//        return list;
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
            logger.error("pubiclfaslfad = " + publicIpType);
            if(publicIpType.equals("none")){
                publicIp = "";
            }else{
                publicIp = createData.get("publicIp").toString();
            }
            inboundPortType = "none";
            inboundPort = null;
        }

        String resourceGroupName = createData.get("resourceGroupName").toString();
//        String customImage = params.get("customImage").toString();

        String imageOS = createData.get("osType").toString();
        String imageId = createData.get("imageId").toString();

        String subnet = createData.get("subnetName").toString();

        String network = createData.get("networkId").toString();

        //Step#1.Server Name
        VirtualMachine.DefinitionStages.Blank regionStage = azure.virtualMachines().define(name);

        //Step#2.Region
        VirtualMachine.DefinitionStages.WithGroup resourceGroupStage = regionStage.withRegion(region);

        //Step#3.Resource Group
        VirtualMachine.DefinitionStages.WithNetwork networkStage = null;
        if (resourceGroupType.equals("exist")) {
            networkStage = resourceGroupStage.withExistingResourceGroup(resourceGroupName);
        } else if (resourceGroupType.equals("new")) {
            networkStage = resourceGroupStage.withNewResourceGroup(resourceGroupName);
        } else {
            networkStage = resourceGroupStage.withNewResourceGroup();
        }

        //Step#4.Primary Network
        VirtualMachine.DefinitionStages.WithPrivateIP privateIpStage = null;
        if (networkType.equals("exist")) {
            VirtualMachine.DefinitionStages.WithSubnet subnetStage = networkStage.withExistingPrimaryNetwork(azure.networks().getById(network));
            privateIpStage = subnetStage.withSubnet(subnet);
        } else if(networkType.equals("new")) {
            privateIpStage = networkStage.withNewPrimaryNetwork(network);
        }

        //Step#4.Private IP
        VirtualMachine.DefinitionStages.WithPublicIPAddress publicIpStage = privateIpStage.withPrimaryPrivateIPAddressDynamic();

        //Step#5. Public IP
        VirtualMachine.DefinitionStages.WithOS imageStage = null;
        if (publicIpType.equals("exist")) {
            imageStage = publicIpStage.withExistingPrimaryPublicIPAddress(azure.publicIPAddresses().getById(publicIp));
        } else if (publicIpType.equals("new")) {
            imageStage = publicIpStage.withNewPrimaryPublicIPAddress(publicIp);
        } else {
            imageStage = publicIpStage.withoutPrimaryPublicIPAddress();
        }

        //Step#6. Image&Credential
        VirtualMachine.DefinitionStages.WithLinuxCreateManaged linuxCreateStage = null;
        VirtualMachine.DefinitionStages.WithWindowsCreateManaged windowsCreate = null;
        VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged publicLinuxCreateStage = null;
        VirtualMachine.DefinitionStages.WithWindowsCreateManagedOrUnmanaged publicWindowsCreate = null;

        if(imageType.equals("custom")) {
            if (imageOS.equals("Linux")) {
                // VirtualMachine.DefinitionStages.WithLinuxRootUsernameManaged usernameStage = imageStage.withLinuxCustomImage(customImage);
                VirtualMachine.DefinitionStages.WithLinuxRootUsernameManaged usernameStage = imageStage.withLinuxCustomImage(imageId);
                VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManaged passwordStage = usernameStage.withRootUsername(username);
                linuxCreateStage = passwordStage.withRootPassword(password);
            } else {
                // VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManaged usernameStage = imageStage.withWindowsCustomImage(customImage);
                VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManaged usernameStage = imageStage.withWindowsCustomImage(imageId);
                VirtualMachine.DefinitionStages.WithWindowsAdminPasswordManaged passwordStage = usernameStage.withAdminUsername(username);
                windowsCreate = passwordStage.withAdminPassword(password);
            }
        } else if(imageType.equals("public")) {

            // URN 형태로 넘어오는 이미지 아이디 정보 파싱
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

        //Step#7. Size(Specification)
        if(imageType.equals("custom")) {
            if (linuxCreateStage != null) {
                //server = linuxCreateStage.withSize(size).create();
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

        //Step#8. Inbound Port Rules
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

        //Step#9. User Script
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
            // server == virtualMachine
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
            logger.error("-------- jsonArray -------- ==== " + jsonArray);
            return jsonArray;
        }
        else {
            logger.error("-------- jsonArray2 -------- ==== " + jsonArray2);
            //생성 성공 시 리턴 값이 없다 하면
            return null;
            //생성 성공 시 리턴 값이 필요 하다 하면
//            return jsonArray2;
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
            //생성 성공 시 리턴 값이 없다 하면
            return null;
            //생성 성공 시 리턴 값이 필요 하다 하면
//            return jsonArray2;
        }
    }

    public ServerInfo start(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        VirtualMachine virtualMachine = azure.virtualMachines().getById(serverId);
        virtualMachine.start();

        ServerInfo info = null;

        info = new ServerInfo(virtualMachine);
        return info;
    }

    public ServerInfo stop(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        VirtualMachine virtualMachine = azure.virtualMachines().getById(serverId);
        virtualMachine.deallocate();

        ServerInfo info = null;

        info = new ServerInfo(virtualMachine);
        return info;
    }

    public ServerInfo reboot(CredentialInfo credentialInfo, String serverId) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        VirtualMachine virtualMachine = azure.virtualMachines().getById(serverId);
        virtualMachine.restart();

        ServerInfo info = null;

        info = new ServerInfo(virtualMachine);
        return info;
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
//        ServerInfo info = null;
//        info = new ServerInfo();
//        info.setId(serverId);
    }

    //    public void deleteDisk(CredentialInfo credentialInfo, String serverId, String diskId) {
    public DeleteInfo deleteDisk(CredentialInfo credentialInfo, String volumeId, Boolean webCheck) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        logger.error("volumeId = " + volumeId);

        try {
            Disk diskInfo = azure.disks().getById(volumeId);

            DeleteInfo deleteinfo = new DeleteInfo();

            deleteinfo.setId(diskInfo.id());
            deleteinfo.setName(diskInfo.name());
            azure.disks().deleteById(volumeId);
//            logger.error("deletedisk, azure, deleteinfo : {}", deleteinfo);

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


    public List<ImageInfo> getImages(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        PagedList<VirtualMachineCustomImage> images = azure.virtualMachineCustomImages().list();
        List<ImageInfo> list = new ArrayList<>();
        for (VirtualMachineCustomImage vmCustomImage : images) {
            ImageInfo info = new ImageInfo(vmCustomImage);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(info);
        }
        return list;
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
//                info.setSubscriptionId(this.subscriptionId);
//                info.setSubscriptionDisplayName(this.subscriptionDisplayName);
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
//            logger.error("-------- jsonArray -------- ==== " + jsonArray);
            return jsonArray;
        }
        else {
//            logger.error("-------- jsonArray2 -------- ==== " + jsonArray2);
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
//            diskInfo.setSubscriptionId(this.subscriptionId);
//            diskInfo.setSubscriptionDisplayName(this.subscriptionDisplayName);
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
//            logger.error("-------- jsonArray -------- ==== " + jsonArray);
            return jsonArray;
        }
        else {
//            logger.error("-------- jsonArray2 -------- ==== " + jsonArray2);
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
//            logger.error("-------- jsonArray -------- ==== " + jsonArray);
            return jsonArray;
        }
        else {
//            logger.error("-------- jsonArray2 -------- ==== " + jsonArray2);
            return jsonArray2;
        }
//        return list;
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

        System.out.println("moduleName = " + moduleName);
        System.out.println("request = " + request);
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


//            System.out.println("getId = " + info.getId());
//            System.out.println("moduleName1 = " + moduleName);
            String moduleName2 = "/" + moduleName;
//            System.out.println("moduleName2 = " + moduleName2);

            if (webCheck) {
                for (int i = 0; i < info.getId().length(); i++) {

                    if (info.getId() != null && info.getId().equals(moduleName2)) {
//                        System.out.println("list0 = " + list);

                        list.add(info);
//                        return list;
                        break;
                    } else {
//                        System.out.println("list2 = " + list);

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
//            logger.error("-------- jsonArray -------- ==== " + jsonArray);
            return jsonArray;
        }
        else {
//            logger.error("-------- jsonArray2 -------- ==== " + jsonArray2);
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
//        logger.error("info2 : {}", info2);
        try {
            jsonString = mapper.writeValueAsString(info);
            jsonString2 = mapper.writeValueAsString(info2);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        logger.error("jsonString2 : {}", jsonString2);
        JSONArray jsonArray = JSONArray.fromObject(jsonString);
        JSONArray jsonArray2 = JSONArray.fromObject(jsonString2);
        if (webCheck) {
            return jsonArray;
        }else{
            //생성 성공 시 리턴 값이 없다 하면
            return null;
            //생성 성공 시 리턴 값이 필요 하다 하면
//            return jsonArray2;
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

    public Map<String, String> getIsUsableIp(CredentialInfo credentialInfo, String network, String privateIp) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        Network azureNetwork = azure.networks().getById(network);
        Boolean usable = (!(azureNetwork.isPrivateIPAddressInNetwork(privateIp)));
        usable.toString();
        Map<String, String> resultMap = null;
        resultMap.put("usable", usable.toString());
        return resultMap;
    }


    public List<PublicIpInfo> getPublicIps(CredentialInfo credentialInfo, String resourceGroup, String region) {
        if (credentialInfo == null) throw new CredentialException();

        region = region != null ? region : null;
        resourceGroup = resourceGroup != null ? resourceGroup : null;

        Azure azure = Credential(credentialInfo);
        PagedList<PublicIPAddress> publicIPAddresses = azure.publicIPAddresses().list();
        List<PublicIpInfo> list = new ArrayList<>();
        for (PublicIPAddress publicIP : publicIPAddresses) {
            if(region !=  null && !publicIP.region().toString().equals(region)) continue;
            if(resourceGroup !=  null && !publicIP.resourceGroupName().equals(resourceGroup)) continue;

            PublicIpInfo info = new PublicIpInfo(publicIP);
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(info);
        }
        return list;
    }

    public List<LoadBalancerInfo> getLoadBalancers(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        PagedList<LoadBalancer> loadBalancers = azure.loadBalancers().list();
        List<LoadBalancerInfo> list = new ArrayList<>();
        for (LoadBalancer loadBalancer : loadBalancers) {
            LoadBalancerInfo info = new LoadBalancerInfo(loadBalancer);
            if(loadBalancer.publicIPAddressIds()!=null) {
                if (loadBalancer.publicIPAddressIds().size() > 1) {
                    info.setPublicIPAddress(loadBalancer.publicIPAddressIds().size() + " public IP addresses");
                } else {
                    PublicIPAddress publicIPAddress = azure.publicIPAddresses().getById(loadBalancer.publicIPAddressIds().get(0));
                    info.setPublicIPAddress(publicIPAddress.ipAddress() + " (" + publicIPAddress.inner().name() + ")");
                }
            }
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(info);
        }
        return list;
    }

    public List<SecurityGroupInfo> getSecurityGroups(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        PagedList<NetworkSecurityGroup> networkSecurityGroups = azure.networkSecurityGroups().list();
        List<SecurityGroupInfo> list = new ArrayList<>();
        for (NetworkSecurityGroup networkSecurityGroup : networkSecurityGroups) {
            SecurityGroupInfo info = new SecurityGroupInfo(networkSecurityGroup);
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(info);
        }
        return list;
    }

    public List<ActiveDirectoryGroupInfo> getActiveDirectoryGroups(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        PagedList<ActiveDirectoryGroup> activeDirectoryGroups = azure.accessManagement().activeDirectoryGroups().list();
        List<ActiveDirectoryGroupInfo> list = new ArrayList<>();
        for (ActiveDirectoryGroup activeDirectoryGroup : activeDirectoryGroups) {
            ActiveDirectoryGroupInfo info = new ActiveDirectoryGroupInfo(activeDirectoryGroup);
            list.add(info);
        }
        return list;
    }

    public List<ActiveDirectoryUserInfo> getActiveDirectoryUsers(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        PagedList<ActiveDirectoryUser> activeDirectoryUsers = azure.accessManagement().activeDirectoryUsers().list();
        List<ActiveDirectoryUserInfo> list = new ArrayList<>();
        for (ActiveDirectoryUser activeDirectoryUser : activeDirectoryUsers) {
            ActiveDirectoryUserInfo info = new ActiveDirectoryUserInfo(activeDirectoryUser);
            list.add(info);
        }
        return list;
    }

    public void getKeyVaults(CredentialInfo credentialInfo) {

        Azure azure = Credential(credentialInfo);

        azure.subscriptions();
    }

    public List<StorageAccountInfo> getStorageAccounts(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        PagedList<StorageAccount> storageAccounts = azure.storageAccounts().list();
        List<StorageAccountInfo> list = new ArrayList<>();
        for (StorageAccount storageAccount : storageAccounts) {
            StorageAccountInfo info = new StorageAccountInfo(storageAccount);
            info.setSubscriptionId(this.subscriptionId);
            info.setSubscriptionDisplayName(this.subscriptionDisplayName);
            list.add(info);
        }
        return list;
    }

    public List<GenericResourceInfo> getGenericResources(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        PagedList<GenericResource> genericResources = azure.genericResources().list();
        List<GenericResourceInfo> list = new ArrayList<>();
        for (GenericResource genericResource : genericResources) {
            GenericResourceInfo info = new GenericResourceInfo(genericResource);
            list.add(info);
        }
        return list;
    }

    public Map<String, Map> getDashboard(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        Map<String, Map> resultMap = new HashMap<>();

        resultMap.put("genericResourcesSummary", getGenericResourcesSummary(credentialInfo));
        resultMap.put("storageSummary", getStorageSummary(credentialInfo));
        resultMap.put("networkSummary", getNetworkSummary(credentialInfo));
//        resultMap.put("serverSummary", getServerSummary(cloudId));

        return resultMap;
    }

    public Map<String, Integer> getGenericResourcesSummary(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        PagedList<GenericResource> genericResources = azure.genericResources().list();
        List<String> typeList = new ArrayList<>();
        for (GenericResource genericResource : genericResources) {
            typeList.add(genericResource.resourceType());
        }
        Map<String, Integer> countMap = new HashMap<>();
        typeList.forEach(e -> {
            Integer count = countMap.get(e);
            countMap.put(e, count == null ? 1 : count + 1);
        });
        return countMap;
    }

    public Map<String, Integer> getStorageSummary(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Map<String, Integer> resultMap = new HashMap<>();

        Azure azure = Credential(credentialInfo);

        PagedList<StorageUsage> storageUsages = azure.storageUsages().list();
        int storageValues = 0;
        for (StorageUsage storageUsage : storageUsages) {
            storageValues += storageUsage.currentValue();
        }
        resultMap.put("storageUsage", storageValues);

        PagedList<Disk> disks = azure.disks().list();
        int diskTotal = 0;
        for (Disk disk : disks) {
            diskTotal += disk.sizeInGB();
        }
        resultMap.put("diskTotalinGB", diskTotal);
        resultMap.put("diskCount", storageUsages.size());
        return resultMap;
    }

    public Map<String, Integer> getNetworkSummary(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Map<String, Integer> resultMap = new HashMap<>();

        Azure azure = Credential(credentialInfo);

        resultMap.put("privateNetworkCount", azure.networks().list().size());
        resultMap.put("loadBalancerCount", azure.loadBalancers().list().size());
        resultMap.put("networkIpCount", azure.publicIPAddresses().list().size());
        return resultMap;
    }

    public ResourceInfo getResourceUsage(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);
        PagedList<VirtualMachine> virtualMachines = azure.virtualMachines().list();
        int running = 0;
        int stopped = 0;
        int etc = 0;
        ResourceInfo info = new ResourceInfo();
        for (VirtualMachine virtualMachine : virtualMachines) {
            if (virtualMachine.powerState() != null) {
                String powerState = virtualMachine.powerState().toString();
                if (powerState.equals("PowerState/running")) {
                    running++;
                }else if(powerState.equals("PowerState/deallocated")){
                    stopped++;
                } else {
                    etc++;
                }
            } else{
                etc++;
            }
        }
        info.setPrivateNetworks(azure.networks().list().size());
        info.setLoadBalancers(azure.loadBalancers().list().size());
        info.setPublicIps(azure.publicIPAddresses().list().size());
        info.setSecurityGroups(azure.networkSecurityGroups().list().size());
        info.setSubscriptions(azure.subscriptions().list().size());
        info.setImages(azure.virtualMachineCustomImages().list().size());
        info.setActiveDirectoryGroups(azure.accessManagement().activeDirectoryGroups().list().size());
        info.setActiveDirectoryUsers(azure.accessManagement().activeDirectoryUsers().list().size());
        info.setStorageAccounts(azure.storageAccounts().list().size());
        info.setTotal(virtualMachines.size());
        info.setRunning(running);
        info.setStop(stopped);
        info.setEtc(etc);
        info.setDisks(azure.disks().list().size());
        int sizeInGB = 0;
        for(Disk disk:azure.disks().list()){
            sizeInGB+=disk.sizeInGB();
        }
        azure.sqlServers().list();
        info.setDiskUsage(sizeInGB);
        info.setSqlServers(azure.sqlServers().list().size());

        return info;
    }

    public Map<String, Object> getServerMetric(CredentialInfo credentialInfo, RequestMetricInfo requestMetricInfo) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        HashMap<String, Object> metricData = new HashMap<String, Object>();

        Date endDate = requestMetricInfo.getEndDate();
        Date startDate = requestMetricInfo.getStartDate();
        DateTime endTime = new DateTime(endDate);
        DateTime startTime = new DateTime(startDate);
        Period interval = Period.minutes(Integer.parseInt(requestMetricInfo.getInterval().toString()) / 60);  // 5minute
        String aggregation = requestMetricInfo.getStatistic();
        String serverId = requestMetricInfo.getId();
        String metricName = requestMetricInfo.getMetricName();

        String[] metric = null;
        switch (metricName){
            case "CPUUtilization" :
                metric = new String[]{"Percentage CPU"};
                break;
            case "NetworkByte" :
                metric = new String[]{"Network In Total", "Network Out Total"};
                break;
            case "DiskBytes" :
                metric = new String[]{"Disk Read Bytes", "Disk Write Bytes"};
                break;
            case "DiskOps" :
                metric = new String[]{"Disk Read Operations/Sec", "Disk Write Operations/Sec"};
                break;
            default:
                metric = new String[]{"Percentage CPU", "Network In Total", "Network Out Total", "Disk Read Bytes", "Disk Write Bytes", "Disk Read Operations/Sec", "Disk Write Operations/Sec"};
                break;
        }

        for (MetricDefinition metricDefinition : azure.metricDefinitions().listByResource(serverId)) {
            if (useLoop(metric, metricDefinition.name().value())) {
                if (metricDefinition.name().value().contains("Network") || metricDefinition.name().value().contains("Bytes")) {
                    aggregation = "total";
                }

                MetricCollection metricCollection = metricDefinition.defineQuery()
                        .startingFrom(startTime)
                        .endsBefore(endTime)
                        .withAggregation(aggregation)
                        .withInterval(interval)
                        .execute();

                if (metricCollection.inner().value().get(0).timeseries().size() != 0) {
                    List<Object> value = new ArrayList<>();
                    for (MetricValue metricValue : metricCollection.inner().value().get(0).timeseries().get(0).data()) {
                        if (metricValue.average() != null) {
                            value.add(metricValue.average());
                        } else if (metricValue.total() != null) {
                            value.add(metricValue.total());
                        } else if (metricValue.count() != null) {
                            value.add(metricValue.count());
                        } else if (metricValue.maximum() != null) {
                            value.add(metricValue.maximum());
                        } else if (metricValue.minimum() != null) {
                            value.add(metricValue.minimum());
                        }

                    }
                    switch (metricCollection.inner().value().get(0).name().value()) {
                        case "Percentage CPU":
                            metricData.put("CPUUtilizationCPU", value);
                            break;
                        case "Network In Total":
                            metricData.put("NetworkByteInput", value);
                            break;
                        case "Network Out Total":
                            metricData.put("NetworkByteOutput", value);
                            break;
                        case "Disk Read Bytes":
                            metricData.put("DiskBytesRead", value);
                            break;
                        case "Disk Write Bytes":
                            metricData.put("DiskBytesWrite", value);
                            break;
                        case "Disk Read Operations/Sec":
                            metricData.put("DiskOpsRead", value);
                            break;
                        case "Disk Write Operations/Sec":
                            metricData.put("DiskOpsWrite", value);
                            break;
                    }
                } else {
                    metricData.put(metricCollection.inner().value().get(0).name().value(), null);
                }

            }
        }
//        metricData = reChangeMetricData(metricData);
        return metricData;
    }

    public List<SubscriptionInfo> getSubscriptions(CredentialInfo credentialInfo){
        if (credentialInfo == null) throw new CredentialException();

        List<SubscriptionInfo> subscriptionList = new ArrayList<>();
        Azure azure = Credential(credentialInfo);
        PagedList<Subscription> subscriptions = azure.subscriptions().list();
        for(Subscription subscription : subscriptions){
            SubscriptionInfo info = new SubscriptionInfo(subscription);
            subscriptionList.add(info);
        }

        return subscriptionList;
    }

    public List<RegionInfo> getRegions(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        List<RegionInfo> regionList = new ArrayList<>();
        Region[] regions = Region.values();
        for(int i=0; i<regions.length; i++) {
            RegionInfo region = new RegionInfo(regions[i]);
            regionList.add(region);
        }

        return regionList;
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

    public VirtualMachineImage getImageDetail(CredentialInfo credentialInfo, String region, String imageId) {
        if (credentialInfo == null) throw new CredentialException();

        Azure azure = Credential(credentialInfo);

        // URN 형태로 넘어오는 이미지 아이디 정보 파싱
        String[] urnArr = imageId.split(":");
        if(urnArr.length != 4) return null;

        String publisher = urnArr[0];
        String offer = urnArr[1];
        String sku = urnArr[2];
        String version = urnArr[3];

        VirtualMachineImage image = null;
        try {
            image = azure.virtualMachineImages().getImage(region, publisher, offer, sku, version);
        } catch(CloudException e) {
            // 지원하지 않은 Region 에 대한 NoRegisteredProviderFound 에러 처리 (그 이외의 에러에 대한 이벤트 발생 처리)
            String errCode = e.body().code();
            if(!errCode.equals("NoRegisteredProviderFound")) {
                e.printStackTrace();
            }
            logger.error("Failed to get azure ImageDetail : '{}'", e.getMessage());
        }
        return image;
    }

    @Override
    public List<ImageDetailInfo> getPublicImages(CredentialInfo credentialInfo) {
        if (credentialInfo == null) throw new CredentialException();

        return null;
    }

    public boolean useLoop(String[] arr, String targetValue) {

        for (String s : arr) {

            if (s.equals(targetValue))

                return true;

        }

        return false;

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
                credentialInfo.setType(list.get(i).getType() == "azure" ? "2" : "2");
                credentialInfo.setTenant(list.get(i).getTenant());
                credentialInfo.setAccessId(list.get(i).getAccessId());
                credentialInfo.setAccessToken(list.get(i).getAccessToken());
                credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
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
}
