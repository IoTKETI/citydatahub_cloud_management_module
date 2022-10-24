package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.io.Serializable;

@Data
public class SubnetInfo implements Serializable {

    private static final long serialVersionUID = -6748814199673902501L;
    private String id;
    private String vpcId;
    private String ownerId;
    private String availabilityZone;
    private String availabilityZoneId;
    private int availableIpAddressCount;
    private String cidrBlock;
    private String subnetArn;
    private Boolean defaultForAz;
    private Boolean mapPublicIpOnLaunch;
    private Boolean assignIpv6AddressOnCreation;
    private String state;

    public SubnetInfo() {

    }

    public SubnetInfo(Subnet info) {
        this.id = info.subnetId();
        this.vpcId = info.vpcId();
        this.ownerId = info.ownerId();
        this.availabilityZone = info.availabilityZone();
        this.availabilityZoneId = info.availabilityZoneId();
        this.availableIpAddressCount = info.availableIpAddressCount();
        this.cidrBlock = info.cidrBlock();
        this.subnetArn = info.subnetArn();
        this.defaultForAz = info.defaultForAz();
        this.mapPublicIpOnLaunch = info.mapPublicIpOnLaunch();
        this.assignIpv6AddressOnCreation = info.assignIpv6AddressOnCreation();
        this.state = info.state().toString();
    }
}
