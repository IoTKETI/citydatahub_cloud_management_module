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
public class ServerInfo implements Serializable {

    private static final long serialVersionUID = -6558252110638995815L;
    private String id;
    private String name;
    private String state;
    private String imageId;
    private String flavorId;
    private int cpu;
    private int memory;
    private int disk;
    private String privateIp;
    private String privateDns;
    private String publicIp;
    private String publicDns;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    private String availabilityZone;
    private String monitoring;
    private String keyName;
    private String securityGroups;

    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ServerInfo() {
    }

    public ServerInfo(Instance info) {
        this.id = info.instanceId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        else this.name = "";
        this.state = info.state().name().toString();
        this.imageId = info.imageId();
        this.flavorId = info.instanceType().toString();
        this.cpu = info.cpuOptions().coreCount();
        this.monitoring = info.monitoring().stateAsString();
        this.availabilityZone = info.placement().availabilityZone();
        this.keyName = info.keyName();
        if(info.securityGroups().size() > 0) this.securityGroups = info.securityGroups().get(0).groupName();
        this.privateIp = info.privateIpAddress();
        this.privateDns = info.privateDnsName();
        this.publicIp = info.publicIpAddress();
        this.publicDns = info.publicDnsName();
        this.createdAt = Timestamp.from(info.launchTime());
    }
}
