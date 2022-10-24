package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.graphrbac.ActiveDirectoryGroup;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ActiveDirectoryGroupInfo implements Serializable {

    private static final long serialVersionUID = -8513758278840067344L;
    private String id;
    private String name;
    private String objectId;
    private Map<String,String> tags;

    public ActiveDirectoryGroupInfo() {

    }

    public ActiveDirectoryGroupInfo(ActiveDirectoryGroup activeDirectoryGroup){
        this.id = activeDirectoryGroup.id();
        this.name = activeDirectoryGroup.name();
        activeDirectoryGroup.inner().displayName();
        this.objectId = activeDirectoryGroup.inner().objectId();
    }
}
