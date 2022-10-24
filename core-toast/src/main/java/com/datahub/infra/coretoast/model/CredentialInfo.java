package com.datahub.infra.coretoast.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CredentialInfo implements Serializable {


    private static final long serialVersionUID = -4468599970527324982L;
    private String id;
    private String name;
    private String region;
    private String tenant;
    private String accessId;
    private String accessToken;
}
