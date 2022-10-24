package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class ServerMonitoringInfo implements Serializable {

    private static final long serialVersionUID = -4259095836589611641L;
    private String id;
    private String statistics;
    private Integer period;
    private String metricName;
    private String unit;
    private Double value;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp timestamp;

    public ServerMonitoringInfo() {
    }
}
