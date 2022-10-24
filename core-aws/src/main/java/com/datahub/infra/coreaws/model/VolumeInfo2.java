package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VolumeInfo2 implements Serializable {

    private static final long serialVersionUID = 4215809178039140702L;
    private String id;
    private String volumeType;
    @JsonProperty("region")
    private String zone;
    private int encrypted;
    @JsonProperty("volumeSize")
    private int size;
    @JsonProperty("IOPS")
    private Integer iops;
    @JsonProperty("volumeStatus")
    private String state;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;

    public VolumeInfo2() {

    }

    public VolumeInfo2(Volume info) {
        this.id = info.volumeId();
        this.volumeType = info.volumeType().toString();
        if(info.iops() != null){
            this.iops = info.iops();
        }else{
            this.iops = null;
        }
        this.state = checkState(info.state().toString());
        this.size = info.size();
        this.zone = info.availabilityZone();
        if(info.encrypted()){
            this.encrypted = 1;
        }
        else{
            this.encrypted = 0;
        }
//        this.encrypted = info.encrypted();
        this.createdAt = Timestamp.from(info.createTime());
    }

    public String checkState(String state){
        if ("in-use".equals(state)) {
            return "using";
        }
        return state;
    }
}

