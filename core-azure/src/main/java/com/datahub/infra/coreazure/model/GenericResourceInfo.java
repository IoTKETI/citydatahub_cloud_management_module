package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.resources.GenericResource;
import lombok.Data;

import java.io.Serializable;

@Data
public class GenericResourceInfo implements Serializable {

    private static final long serialVersionUID = 7864546271581107573L;
    private String id;
    private String name;
    private String type;
    private String location;
    private String resourceGroupName;

    public GenericResourceInfo() {

    }

    public GenericResourceInfo(GenericResource genericResource){
        this.id = genericResource.id();
        String name = genericResource.name();
        this.name = name.split("/")[name.split("/").length-1];
        this.type = genericResource.resourceType();
        this.location = genericResource.inner().location();
        this.resourceGroupName = genericResource.resourceGroupName();
    }
}
