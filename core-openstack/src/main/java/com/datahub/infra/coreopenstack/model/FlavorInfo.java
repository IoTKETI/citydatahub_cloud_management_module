package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.compute.Flavor;

import java.io.Serializable;

@Data
public class FlavorInfo implements Serializable {

    private static final long serialVersionUID = 5167797152043850528L;
    private String id;
    private String name;
    private int vcpus;
    private int ram;
    private int disk;
    private int ephemeral;
    private int swap;
    private float rxtxFactor;
    private Boolean isPublic;

    public FlavorInfo() {

    }

    public FlavorInfo(Flavor flavor) {
        this.id = flavor.getId();
        this.name = flavor.getName();
        this.vcpus = flavor.getVcpus();
        this.ram = flavor.getRam();
        this.disk = flavor.getDisk();
        this.ephemeral = flavor.getEphemeral();
        this.swap = flavor.getSwap();
        this.rxtxFactor = flavor.getRxtxFactor();
        this.isPublic = flavor.isPublic();
    }
}
