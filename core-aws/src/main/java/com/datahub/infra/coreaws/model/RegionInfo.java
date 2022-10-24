package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Region;

import java.io.Serializable;

@Data
public class RegionInfo implements Serializable {
    private static final long serialVersionUID = -2526507532785649537L;
    private String endpoint;
    private String regionName;

    public RegionInfo() {
    }

    public RegionInfo(Region region) {
        this.endpoint = region.endpoint();
        this.regionName = region.regionName();
    }
}

