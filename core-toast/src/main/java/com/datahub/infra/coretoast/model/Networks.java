package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Networks implements Serializable {
    private static final long serialVersionUID = 1108017235251767743L;
    @Key private String name;
    @Key private String id;
    @Key private String status;
    @Key private Boolean shared;
    @Key private List<String> subnets;
    @Key private Boolean admin_state_up;
    @Key private Boolean port_security_enabled;
//    @Key private Boolean router_external;
    @Key private String tenant_id;
    @Key private Integer mtu;
}
