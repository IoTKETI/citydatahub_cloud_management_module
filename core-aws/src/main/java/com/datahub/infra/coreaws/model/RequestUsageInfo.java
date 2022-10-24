package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestUsageInfo implements Serializable {
    private static final long serialVersionUID = -3326667465427085986L;
    private String startDate;
    private String endDate;
    private String granularity;

    public RequestUsageInfo() {
    }
}
