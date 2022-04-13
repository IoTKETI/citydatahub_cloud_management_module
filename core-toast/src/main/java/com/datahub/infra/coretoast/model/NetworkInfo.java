package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class NetworkInfo implements Serializable {
    private static final long serialVersionUID = 2585570205372752303L;
    @Key private Networks[] networks;
    public NetworkInfo(){}
}
