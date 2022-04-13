package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class Security_group_rules implements Serializable {
    private static final long serialVersionUID = -947833152860832471L;
    @Key private String id;
    @Key private String direction;
    @Key private String ethertype;
    @Key private String protocol;
    @Key private String description;
    @Key private String port_range_min;
    @Key private String port_range_max;
    @Key private String remote_group_id;
    @Key private String remote_ip_prefix;
    @Key private String security_group_id;
    @Key private String project_id;
    @Key private String tenant_id;
}
