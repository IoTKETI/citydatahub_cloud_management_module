package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestUsageInfo implements Serializable {
    private static final long serialVersionUID = 2954866201990046455L;
    private String startDate;
    private String endDate;

    public RequestUsageInfo() {
    }
}
