package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;

import java.io.Serializable;

@Data
public class ZoneInfo implements Serializable {
    private static final long serialVersionUID = -2526507532785649537L;
    private String regionName;
    private String zoneName;
    private String zoneId;

    public ZoneInfo() {
    }

    public ZoneInfo(AvailabilityZone zone) {
        this.regionName = zone.regionName();
        this.zoneName = zone.zoneName();
        this.zoneId = zone.zoneId();
    }
}

