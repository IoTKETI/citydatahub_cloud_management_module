package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateServerInfo implements Serializable {

    private static final long serialVersionUID = -6526225975056647858L;

    private String name;
    private String region;
    private String username;
    private String password;
    private String resourceGroupType;
    private String resourceGroupName;
    private String size;
    private String imageType;
    private String imageId;
    private String imageOS;
    private String subnet;
    private String networkType;
    private String network;
    private String publicIpType;
    private String publicIp;
    private String inboundPortType;
    private List<String> inboundPort;
    private String script;
    private Boolean base64Encoded;

    public CreateServerInfo() {
    }
}
