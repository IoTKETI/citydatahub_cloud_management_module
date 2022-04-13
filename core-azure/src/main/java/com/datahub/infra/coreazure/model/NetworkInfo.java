package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.network.Network;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class NetworkInfo implements Serializable {
    private static final long serialVersionUID = 3535798127771589299L;
    private String id;
    private String name;
    private String resourceGroupName;
    private String location;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private List<String> addressSpaces;
    private List<String> dnsServerIPs;
    private Map<String,String> tags;

    public NetworkInfo(){

    }

    public NetworkInfo(Network network) {
        this.id = network.id();
        this.name = network.name();
        this.location = network.regionName();
        this.resourceGroupName = network.resourceGroupName();
        this.addressSpaces = network.addressSpaces();
        this.dnsServerIPs = network.dnsServerIPs();
        this.tags = network.tags().size() != 0 ? network.tags() : null;
    }
}
