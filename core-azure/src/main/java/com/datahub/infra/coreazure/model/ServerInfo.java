package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.compute.VirtualMachine;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ServerInfo implements Serializable {

    private static final long serialVersionUID = -2097698631127792498L;
    private String id;
    private String name;
    private String location;
    private String powerState;
    private String provisioningState;
    private String resourceGroupName;
    private String osType;
    private String primaryPublicIPAddress;
    private String primaryPrivateIP;
    private String size;
    private Map<String,String> tags;
    private String type;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String createId;

    public ServerInfo (){

    }
    public ServerInfo(VirtualMachine vm){
        this.id = vm.id();
        this.name = vm.inner().name();
        this.location = vm.inner().location();
        this.type = vm.type().split("/")[1];
        if(vm.powerState()!=null) this.powerState = vm.powerState().toString().split("/")[1];
        this.provisioningState = vm.provisioningState();
        this.resourceGroupName = vm.resourceGroupName();
        this.osType = vm.osType().toString();
        if(vm.getPrimaryPublicIPAddress() != null) this.primaryPublicIPAddress=vm.getPrimaryPublicIPAddress().ipAddress();
        this.primaryPrivateIP=vm.getPrimaryNetworkInterface().primaryPrivateIP();
        this.size = vm.size().toString();
        this.tags = vm.tags().size() != 0 ? vm.tags() : null;
    }
}
