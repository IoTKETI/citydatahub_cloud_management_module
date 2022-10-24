package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CostDashboardnfo implements Serializable {

    private static final long serialVersionUID = 2031917231174749294L;
    private Double blendedCost;
    private Double unblendedCost;
    private Double amortizedCost;
    private Double normalizedUsageAmount;
    private Double usageQuantity;
    private Double netUnblendedCost;
    private Double netAmortizedCost;


    public CostDashboardnfo() {

    }
}
