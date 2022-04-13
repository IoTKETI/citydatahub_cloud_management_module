package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.identity.v3.Project;

import java.io.Serializable;

@Data
public class ProjectInfo implements Serializable {

    private static final long serialVersionUID = -5138348088546614746L;
    private String id;
    private String description;
    private String name;
    private String parentId;
    private Boolean enabled;

    public ProjectInfo() {

    }

    public ProjectInfo(Project info) {
        this.id = info.getId();
        this.name = info.getName();
        this.description = info.getDescription();
        this.parentId = info.getParentId();
        this.enabled = info.isEnabled();
    }
}
