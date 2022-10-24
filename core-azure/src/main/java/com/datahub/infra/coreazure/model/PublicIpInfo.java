package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.network.PublicIPAddress;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class PublicIpInfo implements Serializable {
    private static final long serialVersionUID = 1097683962079461340L;
    private String id;
    private String name;
    private String resourceGroupName;
    private String location;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String ipAddress;
    private String associatedTo;
    private String sku;
    private String dnsName;
    private Map<String,String> tags;

    public PublicIpInfo() {

    }

    public PublicIpInfo(PublicIPAddress publicIP){
        this.id = publicIP.id();
        this.name = publicIP.name();
        this.resourceGroupName = publicIP.resourceGroupName();
        this.location = publicIP.inner().location();
        this.ipAddress = publicIP.ipAddress();
        if(publicIP.hasAssignedNetworkInterface()) {
            this.associatedTo = publicIP.getAssignedNetworkInterfaceIPConfiguration().parent().name();
        }else if(publicIP.hasAssignedLoadBalancer()){
            this.associatedTo = publicIP.getAssignedLoadBalancerFrontend().parent().name();
        }
        this.tags = publicIP.tags().size() != 0 ? publicIP.tags() : null;
        this.sku = publicIP.inner().sku().name().toString();
        if(publicIP.inner().dnsSettings()!=null) this.dnsName = publicIP.inner().dnsSettings().fqdn();
    }

}
