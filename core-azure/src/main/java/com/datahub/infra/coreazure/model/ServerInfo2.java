package com.datahub.infra.coreazure.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreazure.util.JsonDateDeserializer;
import com.datahub.infra.coreazure.util.JsonDateSerializer;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.PublicIPAddress;
import lombok.Data;

import java.io.Serializable;
import java.text.SimpleDateFormat;

@Data
public class ServerInfo2 implements Serializable {

    private static final long serialVersionUID = -2097698631127792498L;
    private String id;
    private String name;
    private String region;
    private String resourceGroupName;
    private String flavorName;
    private String imageType;
    private String osType;
    private String imageId;
    private String subnetName;
    private String networkId;
    private String publicIpType;
    private String publicIp;
    private String serverState;
    private int cpu;
    private double memory;
    private int disk;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private String createAt;
    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ServerInfo2 (){

    }
    public ServerInfo2(VirtualMachine vm,
                       PagedList<VirtualMachine> vms,
                       PagedList<Network> networks,
                       PagedList<VirtualMachineCustomImage> images,
                       PagedList<PublicIPAddress> publicIPAddresses,
                       PagedList<ComputeSku> skuList,
                       PagedList<Disk> disks){
        this.id = java.util.Base64.getEncoder().encodeToString(vm.id().getBytes());
        this.name = vm.inner().name();
        this.region = vm.inner().location();
        this.resourceGroupName = vm.resourceGroupName();
        this.flavorName = vm.size().toString();
        this.osType = vm.inner().storageProfile().osDisk().osType().toString();
        this.imageId = vm.osDiskId();
        if(vm.powerState()!=null) this.serverState = checkState(vm.powerState().toString().split("/")[1]);

        for(Network temp : networks){
            if(temp.resourceGroupName().toUpperCase().equals(this.getResourceGroupName())) {
                this.networkId = temp.id();
                this.subnetName = temp.subnets().values().iterator().next().toString();
                break;
            }
        }

        for(VirtualMachineCustomImage image : images){
            if(image.id().equals(vm.inner().storageProfile().imageReference().id())){
                this.imageType = "custom";
                break;
            }
        }
        if(this.getImageType() == null){
            this.imageType = "public";
        }

        for (PublicIPAddress publicIP : publicIPAddresses) {
            if(publicIP.resourceGroupName().toUpperCase().equals(this.getResourceGroupName())){
                for(VirtualMachine vm2 : vms){
                    if(vm2.resourceGroupName().equals(publicIP.resourceGroupName().toUpperCase())){
                        this.publicIpType ="exist";
                        this.publicIp = publicIP.ipAddress();
                        break;
                    }
                }
                if(this.getPublicIpType() == null){
                    this.publicIpType = "new";
                    this.publicIp = publicIP.ipAddress();
                }
            }
        }
        if(this.getPublicIpType() == null){
            this.publicIpType = "none";
        }

        for(ComputeSku sku : skuList) {
            if (sku.resourceType().equals(ComputeResourceType.VIRTUALMACHINES)) {
                SizeInfo vmSizeInfo = new SizeInfo(sku);
                String equ = vmSizeInfo.getOffering() + "_" + vmSizeInfo.getVmSize();
                if(vm.size().toString().equals(equ)){
                    this.cpu = vmSizeInfo.getVCPUs();
                    this.memory = vmSizeInfo.getMemoryGB();
                }
            }
        }

        for(Disk disk : disks){
            if(disk.id().equals(this.getImageId())){
                this.disk = disk.sizeInGB();
                this.createAt = disk.inner().timeCreated().toString();
            }
        }
    }

    public String checkState(String state){
        switch (state){
            case "starting":
                return "pending";

            case "deallocated" :
                return "deallocating";

            default:
                return state;
        }
    }
}