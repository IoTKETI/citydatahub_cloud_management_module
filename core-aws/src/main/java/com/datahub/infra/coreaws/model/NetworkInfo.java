package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

import java.io.Serializable;

@Data
public class NetworkInfo implements Serializable {

    private static final long serialVersionUID = 4899660596254863661L;
    private String id;
    private String name;
    private String subnetId;
    private String vpcId;
    private String availabilityZone;
    private String macAddress;
    private String description;
    private String securityGroup;
    private String ownerId;
    private String state;
    private String privateIpAddress;
    private String privateDnsName;
    private Boolean sourceDestCheck;
    private String attachmentId;
    private String instanceId;
    private String instanceOwnerId;
    private int deviceIndex;
    private String attachmentStatus;
    private Boolean deleteOnTermination;

    public NetworkInfo() {

    }

    public NetworkInfo(NetworkInterface info) {
        this.id = info.networkInterfaceId();
        if(info.tagSet().size() > 0) this.name = info.tagSet().get(0).value();
        else this.name = "";
        this.subnetId = info.subnetId();
        this.vpcId = info.vpcId();
        this.availabilityZone = info.availabilityZone();
        this.macAddress = info.macAddress();
        this.description = info.description();
        this.securityGroup = info.groups().get(0).groupName();
        this.ownerId = info.ownerId();
        this.state = info.statusAsString();
        if(info.privateIpAddress() != null){
            this.privateIpAddress = info.privateIpAddress();
        }else{
            this.privateIpAddress = null;
        }
        this.privateDnsName = info.privateDnsName();
        this.sourceDestCheck = info.sourceDestCheck();
        if(info.attachment() == null){
            this.attachmentId = "";
            this.instanceId = null;
            this.instanceOwnerId = null;
            this.deviceIndex = 0;
            this.attachmentStatus = "";
            this.deleteOnTermination = false;
        }else{
            this.attachmentId = info.attachment().attachmentId();
            this.instanceId = info.attachment().instanceId();
            this.instanceOwnerId = info.attachment().instanceOwnerId();
            this.deviceIndex = info.attachment().deviceIndex();
            this.attachmentStatus = info.attachment().statusAsString();
            this.deleteOnTermination = info.attachment().deleteOnTermination();
        }

    }
}
