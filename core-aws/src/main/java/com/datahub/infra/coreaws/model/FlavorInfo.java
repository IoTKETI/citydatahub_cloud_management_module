package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class FlavorInfo implements Serializable {
    private static final long serialVersionUID = 8136181742357589879L;
    private String serviceCode;
    private Map<String, Object> product;
    private Map<String, Object> terms;
    private String version;
    private Date publicationDate;

    private String instanceFamily;
    private String instanceType;
    private String operatingSystem;
    private Integer vCPU;
    private String memory;
    private String storage;
    private String networkPerformance;
    private String gpu;
    private String enhancedNetworkingSupported;
    private String location;

    public void setInstanceTypeInfo() {
        Map<String, String> attributes = (Map<String, String>)this.product.get("attributes");

        this.instanceFamily = attributes.get("instanceFamily");
        this.instanceType = attributes.get("instanceType");
        this.operatingSystem = attributes.get("operatingSystem");
        this.vCPU = Integer.valueOf(attributes.get("vcpu"));
        this.memory = attributes.get("memory");
        this.storage = attributes.get("storage");
        this.networkPerformance = attributes.get("networkPerformance");
        this.gpu = attributes.get("gpu");
        this.enhancedNetworkingSupported = attributes.get("enhancedNetworkingSupported");
        this.location = attributes.get("location");
    }
}
