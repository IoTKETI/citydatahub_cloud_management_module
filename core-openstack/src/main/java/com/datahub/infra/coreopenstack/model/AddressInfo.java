package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.compute.Address;

import java.io.Serializable;

@Data
public class AddressInfo implements Serializable {

    private static final long serialVersionUID = 502891212771252804L;
    private String macAddr;
    private int version;
    private String addr;
    private String type;
    private String networkName;

    public AddressInfo() {

    }

    public AddressInfo(Address info) {
        this.macAddr = info.getMacAddr();
        this.version = info.getVersion();
        this.addr = info.getAddr();
        this.type = info.getType();
    }
}
