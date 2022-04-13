package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateServerInfo implements Serializable {

    private static final long serialVersionUID = -570168447680820932L;
    private String imageId;
    private String name;
    private String instanceType;
    private Integer instanceCount;
    private String securityGroup;
    private String keypair;
    private Boolean monitoringEnabled;
    private String subnetId;
    private String script;
    private Boolean base64Encoded;

    private String flavorName;
    private String subnetName;
    private String securityGroupName;
    public CreateServerInfo() {
    }
}
