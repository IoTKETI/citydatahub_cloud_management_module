package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.network.HostRoute;

import java.io.Serializable;

@Data
public class HostRouteInfo implements Serializable {

    private static final long serialVersionUID = -2253011978092719339L;
    private String destination;
    private String nexthop;

    public HostRouteInfo() {

    }

    public HostRouteInfo(HostRoute info) {
        this.destination = info.getDestination();
        this.nexthop = info.getNexthop();
    }
}
