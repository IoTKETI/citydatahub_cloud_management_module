package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class SecurityGroupInfo implements Serializable {
    private static final long serialVersionUID = 6281945088569117175L;
    @Key
    private SecurityGroups security_group;

    public SecurityGroupInfo(){}
}
