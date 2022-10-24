package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class ZoneDetailInfo implements Serializable {

    private static final long serialVersionUID = -8629918608557087127L;

    @Key private ZoneState zoneState;
    @Key private String hosts;
    @Key private String zoneName;


//    @Override
//    public String toString() {
//        return "CreateServerInfo{" +
//                "header=" + createServerInfo +
//                ", instance=" + createServerInfo2 +
//                '}';
//    }
}
