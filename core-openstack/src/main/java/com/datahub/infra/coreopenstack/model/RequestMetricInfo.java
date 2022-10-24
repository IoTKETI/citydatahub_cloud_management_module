package com.datahub.infra.coreopenstack.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestMetricInfo implements Serializable {
    private static final long serialVersionUID = 6221306866939451227L;
    private Integer metricName;
    private String statistic;
    private Integer interval;
    private Long endDate;
    private Long startDate;
    private String id;

    public RequestMetricInfo() {
    }
}
