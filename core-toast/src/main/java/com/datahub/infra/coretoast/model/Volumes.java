package com.datahub.infra.coretoast.model;

import com.datahub.infra.coretoast.util.JsonDateDeserializer;
import com.datahub.infra.coretoast.util.JsonDateSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Volume Model
 */
@Data
public class Volumes implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @JsonProperty("attachedVm")
    @Key private List<Attachments> attachments;
    @Key private List<Links> links;
    @JsonProperty("location")
    @Key private String availability_zone;
    @Key private Boolean encrypted;
    @JsonProperty("volumeType")
    @Key private String volume_type;
    @JsonProperty("snapshotId")
    @Key private String snapshot_id;
    @Key private String id;
    @Key private Integer size;
    @JsonProperty("userId")
    @Key private String user_id;
    @Key private MetadataVolumes metadata;
    @Key private String status;
    @Key private String description;
    @Key private Boolean multiattach;
    @JsonProperty("sourceVolid")
    @Key private String source_volid;
    @JsonProperty("consistencygroupId")
    @Key private String consistencygroup_id;
    @Key private String name;
    @Key private String bootable;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonProperty("createdAt")
    @Key private String created_at;
    @JsonProperty("replicationStatus")
    @Key private String replication_status;


//    @Override
//    public String toString() {
//        return "Volumes{" +
//                "attachments=" + (attachments == null ? "null": Arrays.toString(attachments)) +
//                ", availabilityzone=" + availabilityzone + '\'' +
//                ", createAt=" + createAt + '\'' +
//                ", description=" + description + '\'' +
//                "metadata=" + (metadata == null ? "null": Arrays.toString(metadata)) +
//                ", name=" + name + '\'' +
//                ", size=" + size + '\'' +
//                ", status=" + status + '\'' +
//                ", volumeType=" + volumeType + '\'' +
//                '}';
//    }
}
