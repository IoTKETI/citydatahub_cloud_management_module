package com.datahub.infra.coreazure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreazure.util.JsonDateDeserializer;
import com.datahub.infra.coreazure.util.JsonDateSerializer;
import com.microsoft.azure.management.compute.Disk;
import lombok.Data;

import java.io.Serializable;


@Data
public class DiskInfo2 implements Serializable {
    private static final long serialVersionUID = -4137673447100054200L;
    private String id;
    private String name;
    @JsonProperty("region")
    private String location;
    @JsonProperty("securityGroupName")
    private String resourceGroup;
    private int size;
    @JsonProperty("volumeState")
//    private String isAttached;
    private String isAttached;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonProperty("createdAt")
    private String created;


    public DiskInfo2() {

    }

    public DiskInfo2(Disk disk){
        this.id = java.util.Base64.getEncoder().encodeToString(disk.id().getBytes());
        this.name = disk.name();
        this.location = disk.regionName();
        this.resourceGroup = disk.resourceGroupName();
        this.size = disk.sizeInGB();
        this.isAttached = checkState(String.valueOf(disk.isAttachedToVirtualMachine()));
        this.created = disk.inner().timeCreated().toString();

    }

    public String checkState(String state){
        switch (state){
            case "true" :
                return "using";

            case "false":
                return "notUsing";

            default:
                return state;
        }
    }
}
