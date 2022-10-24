package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.network.LoadBalancer;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class LoadBalancerInfo implements Serializable {
    private static final long serialVersionUID = -5544227925602744068L;
    private String id;
    private String name;
    private String resourceGroup;
    private String location;
    private String sku;
    private String backendPool;
    private int inboundNatRules;
    private int outboundNatRules;
    private List<String> httpProbes;
    private List<String> tcpProbes;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String publicIPAddress;

    private Map<String,String> tags;

    public LoadBalancerInfo() {

    }

    public LoadBalancerInfo(LoadBalancer loadBalancer){
        this.id = loadBalancer.id();
        this.name = loadBalancer.name();
        this.resourceGroup = loadBalancer.resourceGroupName();
        this.location = loadBalancer.regionName();
        this.sku = loadBalancer.inner().sku().name().toString();
        this.inboundNatRules = loadBalancer.inner().inboundNatRules()!=null?loadBalancer.inner().inboundNatRules().size():0;
        this.outboundNatRules = loadBalancer.inner().outboundNatRules()!=null?loadBalancer.inner().outboundNatRules().size():0;

        if(loadBalancer.inner().backendAddressPools().size()>1){
            this.backendPool = loadBalancer.inner().backendAddressPools().size()+" backend pools";
        }else{
            this.backendPool = loadBalancer.inner().backendAddressPools().get(0).name();
        }
        Set tcpkey = loadBalancer.tcpProbes().keySet();
        Iterator iterator1 = tcpkey.iterator();
        List<String> tcpProbes = new ArrayList<>();
        while(iterator1.hasNext()){
            String key = (String)iterator1.next();
            tcpProbes.add(key);

        }
        this.tcpProbes=tcpProbes;


        Set httpkey = loadBalancer.httpProbes().keySet();
        Iterator iterator2 = httpkey.iterator();
        List<String> httpProbes = new ArrayList<>();
        while(iterator2.hasNext()){
            String key = (String)iterator2.next();
            httpProbes.add(key);
        }
        this.httpProbes=httpProbes;
        this.tags = loadBalancer.tags().size() != 0 ? loadBalancer.tags() : null;
    }

}