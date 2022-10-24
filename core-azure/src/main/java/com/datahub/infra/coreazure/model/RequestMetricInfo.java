package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RequestMetricInfo implements Serializable {

    private static final long serialVersionUID = 1760949754609001482L;
    private Date endDate;
    private Date startDate;
    private String name = "InstanceId";
    private String id;
    private String metricName;
    private Integer interval;
    private String statistic;

    public RequestMetricInfo() {
    }
}
