package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import lombok.Data;

import java.io.Serializable;

@Data
public class RegionInfo implements Serializable {
    private static final long serialVersionUID = -2526507532785649537L;
    private String name;
    private String label;

    public RegionInfo() {
    }

    public RegionInfo(Region region) {
        this.name = region.name();
        this.label = region.label();
    }
}
