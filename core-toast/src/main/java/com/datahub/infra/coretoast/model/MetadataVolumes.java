package com.datahub.infra.coretoast.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class MetadataVolumes implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String readonly;
    @JsonProperty("attachedMode")
    @Key private String attached_mode;

//    @Override
//    public String toString() {
//        return "Metadata{" +
//                "key=" + key +
//                '}';
//    }


}
