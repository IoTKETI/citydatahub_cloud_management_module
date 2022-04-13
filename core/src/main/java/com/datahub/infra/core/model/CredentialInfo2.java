package com.datahub.infra.core.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CredentialInfo2 implements Serializable {
    private static final long serialVersionUID = 1641101709095981091L;
    private String name;
    private String cspType;
    private String region;
    private String domain;
    private String url;
    private String tenantId;
    private String accessId;
    private String accessToken;
    private String projectId;
    private String subscriptionId;

    public CredentialInfo2() {}
}
