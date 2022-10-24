package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class VolumesInfo implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

//    @Key private Header header;
    @Key private Volumes[] volumes;

    public VolumesInfo(){}
}
