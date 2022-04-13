package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST VolumeInfo Model
 */
@Data
public class TypeInfo implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

//    @Key private Header header;
    @Key private VolumeType[] volume_types;

    public TypeInfo(){}
}
