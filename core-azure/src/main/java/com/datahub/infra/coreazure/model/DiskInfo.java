package com.datahub.infra.coreazure.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreazure.util.JsonDateDeserializer;
import com.datahub.infra.coreazure.util.JsonDateSerializer;
import com.microsoft.azure.management.compute.Disk;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DiskInfo implements Serializable {
    private static final long serialVersionUID = -4137673447100054200L;
    private String id;
    private String name;
    private String storageAccountType;
    private int size;
    private String ownerVm;
    private String resourceGroup;
    private String location;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private boolean isAttached;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private String created;
    private List<String> availityZone;
    private String os;
    private Map<String,String> tags;

    public DiskInfo() {

    }

    public DiskInfo(Disk disk){
        this.id = disk.id();
        this.name = disk.name();
        this.size = disk.sizeInGB();
        this.storageAccountType = disk.inner().type();
        this.resourceGroup = disk.resourceGroupName();
        this.location = disk.regionName();
        this.isAttached = disk.isAttachedToVirtualMachine();
        if(isAttached){
            this.ownerVm = disk.virtualMachineId().split("/")[disk.virtualMachineId().split("/").length-1];
        }
        this.created = disk.inner().timeCreated().toString();
        this.availityZone = disk.inner().zones();
        if(disk.inner().osType()!=null) {
            this.os = disk.inner().osType().name();
        }
        this.tags = disk.tags().size() != 0 ? disk.tags() : null;
        this.storageAccountType = disk.sku().toString();
    }

}
