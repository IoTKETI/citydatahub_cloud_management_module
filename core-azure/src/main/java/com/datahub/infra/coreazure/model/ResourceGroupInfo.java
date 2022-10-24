package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.resources.ResourceGroup;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ResourceGroupInfo implements Serializable {
    private static final long serialVersionUID = 1384139586369061484L;
    private String name;
    private String id;
    private String type;
    private String location;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private List<GenericResourceInfo> resources;
    private Map<String,String> tags;

    public ResourceGroupInfo() {

    }

    public ResourceGroupInfo(ResourceGroup resourceGroup){
        this.id = resourceGroup.id();
        this.name = resourceGroup.name();
        this.type = resourceGroup.type();
        this.location= resourceGroup.regionName();
        this.tags = resourceGroup.tags().size() != 0 ? resourceGroup.tags() : null;
    }
}
