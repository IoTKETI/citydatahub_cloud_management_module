package com.datahub.infra.core.model;

import com.datahub.infra.core.util.JsonDateDeserializer;
import com.datahub.infra.core.util.JsonDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
public class CredentialInfo implements Serializable {


    private static final long serialVersionUID = -4468599970527324982L;
    private String id;
    private String name;
    private String type;
    private String region;
    private String domain;
    private String url;
    private String tenant;
    private String accessId;
    private String accessToken;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp duration;
    private String projectId;
    private String subscriptionId;
    private String cloudType;

    private String cspType;
    private String tenantId;

    private List<ProjectInfo> projects;

    public CredentialInfo(){}

    public CredentialInfo(CredentialInfo2 info2){
        this.name = info2.getName();
        this.type = info2.getCspType().equals("1") ? "aws" : info2.getCspType().equals("2") ? "azure" : "openstack" ;
        this.region = info2.getRegion();
        this.domain = info2.getDomain();
        this.url = info2.getUrl();
        this.tenant = info2.getTenantId();
        this.accessId = info2.getAccessId();
        this.accessToken = info2.getAccessToken();
        this.subscriptionId = info2.getSubscriptionId();
        this.projectId = info2.getProjectId();
    }
}
