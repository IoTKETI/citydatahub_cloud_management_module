package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.implementation.SecurityRuleInner;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SecurityGroupInfo implements Serializable {
    private static final long serialVersionUID = -9197947898669615096L;
    private String id;
    private String name;
    private String resourceGroupName;
    private String location;
    private Map<String,String> tags;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String customSecurityRules;
    private String associatedWith;
    private List<Map<String,String>> inboundSecourityRules = new ArrayList<>();
    private List<Map<String,String>> outboundSecourityRules = new ArrayList<>();

    public SecurityGroupInfo() {

    }

    public SecurityGroupInfo(NetworkSecurityGroup networkSecurityGroup){
        this.id = networkSecurityGroup.id();
        this.name = networkSecurityGroup.name();
        this.location = networkSecurityGroup.regionName();
        this.resourceGroupName = networkSecurityGroup.resourceGroupName();

        int associatedSubnets = networkSecurityGroup.listAssociatedSubnets()!=null?networkSecurityGroup.listAssociatedSubnets().size():0;
        int associatedNetworkInterfaces = networkSecurityGroup.inner().networkInterfaces()!=null?networkSecurityGroup.inner().networkInterfaces().size():0;
        this.associatedWith = associatedSubnets + " inbound, " + associatedNetworkInterfaces + " outbount";



        this.tags = networkSecurityGroup.tags().size() != 0 ? networkSecurityGroup.tags() : null;

        Map<String, NetworkSecurityRule> securityRules = networkSecurityGroup.securityRules();
        List<SecurityRuleInner> defaultSecurityRules = networkSecurityGroup.inner().defaultSecurityRules();
        List<SecurityRuleInner> innerSecurityRules = networkSecurityGroup.inner().securityRules();
        for(SecurityRuleInner securityRuleInner : innerSecurityRules){
            if(securityRuleInner.direction().toString().equals("Inbound")){
                Map<String,String> rule =new HashMap<>();
                rule.put("priority",securityRuleInner.priority().toString());
                rule.put("name",securityRuleInner.name());
                rule.put("port",securityRuleInner.destinationPortRange());
                rule.put("protocol",securityRuleInner.protocol().toString());
                rule.put("source",securityRuleInner.sourceAddressPrefix());
                rule.put("destination",securityRuleInner.destinationAddressPrefix());
                rule.put("action",securityRuleInner.access().toString());
                this.inboundSecourityRules.add(rule);
            }else{
                Map<String,String> rule =new HashMap<>();
                rule.put("priority",securityRuleInner.priority().toString());
                rule.put("name",securityRuleInner.name());
                rule.put("port",securityRuleInner.destinationPortRange());
                rule.put("protocol",securityRuleInner.protocol().toString());
                rule.put("source",securityRuleInner.sourceAddressPrefix());
                rule.put("destination",securityRuleInner.destinationAddressPrefix());
                rule.put("action",securityRuleInner.access().toString());
                this.outboundSecourityRules.add(rule);
            }

        }
        int customInboundSize = this.inboundSecourityRules.size()!=0?this.inboundSecourityRules.size():0;
        int customOutboundSize = this.outboundSecourityRules.size()!=0?this.outboundSecourityRules.size():0;
        this.customSecurityRules = customInboundSize+ " inbound, " + customOutboundSize + " outbount";

        for(SecurityRuleInner securityRuleInner : defaultSecurityRules){
            if(securityRuleInner.direction().equals("Inbound")){
                Map<String,String> rule =new HashMap<>();
                rule.put("priority",securityRuleInner.priority().toString());
                rule.put("name",securityRuleInner.name());
                rule.put("port",securityRuleInner.destinationPortRange());
                rule.put("protocol",securityRuleInner.protocol().toString());
                rule.put("source",securityRuleInner.sourceAddressPrefix());
                rule.put("destination",securityRuleInner.destinationAddressPrefix());
                rule.put("action",securityRuleInner.access().toString());
                this.inboundSecourityRules.add(rule);
            }else{
                Map<String,String> rule =new HashMap<>();
                rule.put("priority",securityRuleInner.priority().toString());
                rule.put("name",securityRuleInner.name());
                rule.put("port",securityRuleInner.destinationPortRange());
                rule.put("protocol",securityRuleInner.protocol().toString());
                rule.put("source",securityRuleInner.sourceAddressPrefix());
                rule.put("destination",securityRuleInner.destinationAddressPrefix());
                rule.put("action",securityRuleInner.access().toString());
                this.inboundSecourityRules.add(rule);
            }
        }
    }
}