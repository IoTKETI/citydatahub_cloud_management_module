package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class SecurityGroupsInfo implements Serializable {
    private static final long serialVersionUID = 6281945088569117175L;
    @Key
    private SecurityGroups[] security_groups;

    public SecurityGroupsInfo(){}
}
