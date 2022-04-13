package com.datahub.infra.coretoast.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Attachments Model
 */
@Data
public class Attachments implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @JsonProperty("serverId")
    @Key private String server_id;
    @JsonProperty("attachmentId")
    @Key private String attachment_id;
    @JsonProperty("hostName")
    @Key private String host_name;
    @JsonProperty("volumeId")
    @Key private String volume_id;
    @Key private String device;
    @Key private String id;

//    @Override
//    public String toString() {
//        return "attachments{" +
//                "device=" + device + '\'' +
//                ", instanceId=" + instanceId + '\'' +
//                ", attachmentId=" + attachmentId +
//                '}';
//    }
}
