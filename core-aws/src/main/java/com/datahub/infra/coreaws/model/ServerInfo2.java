package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Data
public class ServerInfo2 implements Serializable {
    private static final long serialVersionUID = 4776763910745157219L;
    private String id;
    private String name;
    private String region;
    private int instanceCount;
    private String imageId;
    private String flavorName;
    private String securityGroupName;
    private String keypair;
    private String subnetName;
    private String ip;
    private String serverState;
    int cpu;
    double memory;
    int disk;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ServerInfo2() {
    }

    public ServerInfo2(Instance info) {
        if(info == null) return;
        this.id = info.instanceId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        else this.name = "";
        this.cpu = info.cpuOptions().coreCount();
        this.region = info.placement().availabilityZone();
        this.imageId = info.imageId();
        this.flavorName = info.instanceType().toString();
        if(info.securityGroups().size() > 0) this.securityGroupName = info.securityGroups().get(0).groupName();
        this.keypair = info.keyName();
        this.subnetName = info.subnetId();
        this.ip = info.publicIpAddress();
        this.serverState = checkState(info.state().name().toString());
        this.createdAt = Timestamp.from(info.launchTime());
    }

    public String checkState(String state){
        if ("shutting-down".equals(state)) {
            return "deallocating";
        }
        return state;
    }
}
