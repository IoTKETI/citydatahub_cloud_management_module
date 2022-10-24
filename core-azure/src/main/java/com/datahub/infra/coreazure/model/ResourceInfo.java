package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResourceInfo implements Serializable {

    private static final long serialVersionUID = -2679171806402598417L;
    private int running;
    private int stop;
    private int etc;
    private int total;
    private int subscriptions;
    private int privateNetworks;
    private int loadBalancers;
    private int publicIps;
    private int securityGroups;
    private int images;
    private int storageAccounts;
    private int disks;
    private int diskUsage;
    private int activeDirectoryGroups;
    private int activeDirectoryUsers;
    private int sqlServers;

    public ResourceInfo() {

    }
}
