package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class FloatingipsInfo implements Serializable {
    private static final long serialVersionUID = 6493057709859841687L;
    @Key
    private Floatingips[] floatingips;
    public FloatingipsInfo(){}
}
