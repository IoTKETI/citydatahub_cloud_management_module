package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ResourceUsageDetailInfo implements Serializable {
    private static final long serialVersionUID = 8075787705520772560L;
    String id;
    String name;
    String type;
    Map<String, Object> tags;
    Map<String, Object> properties;

    public ResourceUsageDetailInfo() {

    }

}
