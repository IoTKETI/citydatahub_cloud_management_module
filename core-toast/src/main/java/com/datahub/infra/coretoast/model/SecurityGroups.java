package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class SecurityGroups implements Serializable {
    private static final long serialVersionUID = -6665712448607251742L;
    @Key private String description;
    @Key private String id;
    @Key private String name;
    @Key private Security_group_rules[] security_group_rules;
    @Key private String tenant_id;
}
