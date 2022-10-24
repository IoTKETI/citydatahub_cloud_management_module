package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import java.io.Serializable;

@Data
public class SecurityGroupInfo implements Serializable {

    private static final long serialVersionUID = -5926788500686278103L;
    private String groupId;
    private String name;
    private String vpcId;
    private String description;
    private String groupName;
    private String ownerId;

    public SecurityGroupInfo() {

    }

    public SecurityGroupInfo(SecurityGroup info) {
        this.groupId = info.groupId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        else this.name = "";
        this.vpcId = info.vpcId();
        this.description = info.description();
        this.ownerId = info.ownerId();
        this.groupName = info.groupName();
    }
}
