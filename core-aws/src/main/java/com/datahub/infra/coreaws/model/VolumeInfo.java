package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VolumeInfo implements Serializable {

    private static final long serialVersionUID = 4215809178039140702L;
    private String id;
    private String name;
    private int size;
    private String volumeType;
    private Integer iops;
    private String state;
    private String snapshotId;
    private Boolean encrypted;
    private String zone;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    private String instanceId;
    private String device;
    private String attachState;

    public VolumeInfo() {

    }

    public VolumeInfo(Volume info) {
        this.id = info.volumeId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        this.volumeType = info.volumeType().toString();
        if(info.iops() != null){
            this.iops = info.iops();
        }else{
            this.iops = null;
        }
        this.state = info.state().toString();
        this.size = info.size();
        this.snapshotId = info.snapshotId();
        this.zone = info.availabilityZone();
        this.encrypted = info.encrypted();
        if(info.attachments().isEmpty()){
            this.instanceId = "";
            this.device = "";
            this.attachState = "";
        }else{
            this.instanceId = info.attachments().get(0).instanceId();
            this.device = info.attachments().get(0).device();
            this.attachState = info.attachments().get(0).stateAsString();
        }

        this.createdAt = Timestamp.from(info.createTime());
    }
}
