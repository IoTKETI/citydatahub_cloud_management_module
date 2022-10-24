package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.network.Network;
import lombok.Data;

import java.io.Serializable;

@Data
public class NetworkInfo2 implements Serializable {
    private static final long serialVersionUID = 3535798127771589299L;
    private String id;
    private String name;
    private String ip;
    private String region;
    private String resourceGroupName;
    private String state;

    public NetworkInfo2(){

    }

    public NetworkInfo2(Network network) {
        this.id = java.util.Base64.getEncoder().encodeToString(network.id().getBytes());
//        this.id = network.id();
        this.name = network.name();
        this.ip = network.addressSpaces().toString().replaceAll("[^0-9./]", "");
        this.region = network.regionName();
        this.resourceGroupName = network.resourceGroupName();
        this.state = checkState(network.inner().provisioningState());
    }


    public String checkState(String state){
        switch (state){
            case "Updating" :
                return "pending";

            case "Succeeded":
                return "available";

            default:
                return state;
        }
    }
}
