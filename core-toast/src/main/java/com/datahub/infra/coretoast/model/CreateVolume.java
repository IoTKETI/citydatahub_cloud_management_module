package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateVolume implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String status;
    @Key private List<Attachments> attachments;
    @Key private List<Links> links;
    @Key private String bootable;
    @Key private Boolean encrypted;
    @Key private MetadataVolumes metadata;
    @Key private String id;
    @Key private Integer size;
    @Key private String user_id;
    @Key private String availability_zone;
    @Key private String created_at;
    @Key private String volume_type;

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
