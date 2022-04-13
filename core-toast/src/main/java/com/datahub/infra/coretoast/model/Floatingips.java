package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class Floatingips implements Serializable {
    private static final long serialVersionUID = -6300759761247093918L;
    @Key private String floating_network_id;
    @Key private String router_id;
    @Key private String floating_ip_address;
    @Key private String tenant_id;
    @Key private String status;
    @Key private String port_id;
    @Key private String id;
}
