package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.network.Network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class NetworkInfo implements Serializable {

    private static final long serialVersionUID = -18128771593660073L;
    private String id;
    private String name;
    private Boolean shared;
    private Boolean external;
    private String state;
    private Boolean adminStateUp;
    private List<String> visibilityZones;
    private String projectId;
    private String projectName;

    public NetworkInfo() {

    }

    public NetworkInfo(Network info) {
        if(info != null) {
            this.id = info.getId();
            this.name = info.getName();
            this.shared = info.isShared();
            this.external = info.isRouterExternal();
            this.state = info.getStatus().name();
            this.adminStateUp = info.isAdminStateUp();
            this.visibilityZones = info.getAvailabilityZones();
            this.projectId = info.getTenantId();
        }
    }
}
