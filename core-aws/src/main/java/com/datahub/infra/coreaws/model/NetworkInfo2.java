package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

import java.io.Serializable;

@Data
public class NetworkInfo2 implements Serializable {

    private static final long serialVersionUID = 4899660596254863661L;
    private String id;
    private String name;
    @JsonProperty("ip")
    private String privateIpAddress;
    @JsonProperty("subnetName")
    private String subnetId;
    @JsonProperty("resourceGroupName")
    private String securityGroup;
    private String state;


    public NetworkInfo2() {

    }

    public NetworkInfo2(NetworkInterface info) {
        this.id = info.networkInterfaceId();
        this.name=info.description();
        this.subnetId = info.subnetId();
        this.securityGroup = info.groups().get(0).groupName();
        this.state = info.statusAsString();
        if(info.privateIpAddress() != null){
            this.privateIpAddress = info.privateIpAddress();
        }else{
            this.privateIpAddress = null;
        }



    }
}
