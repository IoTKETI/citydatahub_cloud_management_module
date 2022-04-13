package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class Subnets implements Serializable {
    private static final long serialVersionUID = -2199834068439467064L;
    @Key private String name;
    @Key private String cidr;
    @Key private String id;
    @Key private String network_id;
    @Key private Integer ip_version;
    @Key private String gateway_ip;
    @Key private Boolean enable_dhcp;
    @Key private String tenant_id;
    @Key private Allocation_pools[] allocation_pools;
    @Key private Dns_nameservers[] dns_nameservers;
    @Key private Host_routes[] host_routes;
    @Key private String ipv6_address_model;
    @Key private String subnetpool_id;
    @Key private String ipv6_ra_mode;

}
