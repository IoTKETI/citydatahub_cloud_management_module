package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class ZoneInfoApi implements Serializable {

    private static final long serialVersionUID = -8629918608557087127L;

    @Key private ZoneDetailInfo[] availabilityZoneInfo;

}
