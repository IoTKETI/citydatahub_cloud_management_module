package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.graphrbac.ActiveDirectoryUser;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ActiveDirectoryUserInfo implements Serializable {

    private static final long serialVersionUID = 19637105670339669L;
    private String id;
    private String name;
    private String objectId;
    private Map<String,String> tags;

    public ActiveDirectoryUserInfo(){

    }

    public ActiveDirectoryUserInfo(ActiveDirectoryUser adu){
        this.id = adu.id();
        this.name = adu.name();
        this.objectId = adu.inner().objectId();
        adu.inner().displayName();
        adu.inner().givenName();
        adu.inner().immutableId();
        adu.inner().surname();
        adu.inner().usageLocation();
        adu.inner().userPrincipalName();
        adu.inner().userType();
        adu.inner().signInNames();
    }
}
