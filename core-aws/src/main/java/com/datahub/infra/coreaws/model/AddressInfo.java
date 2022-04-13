package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Address;

import java.io.Serializable;

@Data
public class AddressInfo implements Serializable {

    private static final long serialVersionUID = 5081300051884935797L;
    private String id;
    private String name;
    private String publicIp;
    private String associationId;
    private String instanceId;
    private String domain;
    private String networkInterfaceId;
    private String networkInterfaceOwnerId;
    private String privateIpAddress;
    private String publicIpv4Pool;

    public AddressInfo() {

    }

    public AddressInfo(Address info) {
        this.id = info.allocationId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        this.publicIp = info.publicIp();
        if(info.associationId() != null)this.associationId = info.associationId();
        if(info.instanceId() != null) this.instanceId = info.instanceId();
        this.domain = info.domainAsString();
        if(info.networkInterfaceId() != null)this.networkInterfaceId = info.networkInterfaceId();
        if(info.networkInterfaceOwnerId() != null)this.networkInterfaceOwnerId = info.networkInterfaceOwnerId();
        if(info.privateIpAddress() != null)this.privateIpAddress = info.privateIpAddress();
        this.publicIpv4Pool = info.publicIpv4Pool();
    }
}
