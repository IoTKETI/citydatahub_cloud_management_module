package com.datahub.infra.coreopenstack.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.openstack4j.model.network.Network;

import java.io.Serializable;

@Data
public class NetworkInfo2 implements Serializable {

    private static final long serialVersionUID = -18128771593660073L;
    private String id;
    private String name;
    @JsonProperty("networkManaged")
    private Boolean adminStateUp;
    @JsonProperty("networkShared")
    private Boolean shared;
    @JsonProperty("region")
    private String visibilityZones;
    @JsonProperty("networkState")
    private String state;

    public NetworkInfo2() {

    }

    public NetworkInfo2(Network info) {
        if(info != null) {
            this.id = info.getId();
            this.name = info.getName();
            this.adminStateUp = info.isAdminStateUp();
            this.shared = info.isShared();


            if (info.getAvailabilityZones() != null) {
                for (int i = 0; i < info.getAvailabilityZones().size(); i++) {
                    this.visibilityZones = info.getAvailabilityZones().get(i);
                }
            }
            this.state = checkState(info.getStatus().name());
        }
    }

    public String checkState(String state){
        if ("ACTIVE".equals(state)) {
            return "pending";
        }
        return state;
    }
}
