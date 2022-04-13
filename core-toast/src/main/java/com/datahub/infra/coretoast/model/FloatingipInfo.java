package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class FloatingipInfo implements Serializable {
    private static final long serialVersionUID = 3926120934433309170L;
    @Key
    private Floatingips floatingip;
    public FloatingipInfo(){}
}
