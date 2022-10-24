package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Vpc;

import java.io.Serializable;

@Data
public class VpcInfo implements Serializable {

    private static final long serialVersionUID = -1614394664211098792L;
    private String id;
    private String ownerId;
    private String dhcpOptionsId;
    private String instanceTenancy;
    private String cidrBlock;
    private Boolean isDefault;
    private String state;

    public VpcInfo() {

    }

    public VpcInfo(Vpc info) {
        this.id = info.vpcId();
        this.ownerId = info.ownerId();
        this.dhcpOptionsId = info.dhcpOptionsId();
        this.instanceTenancy = info.instanceTenancy().toString();
        this.cidrBlock = info.cidrBlock();
        this.isDefault = info.isDefault();
        this.state = info.state().toString();
    }
}
