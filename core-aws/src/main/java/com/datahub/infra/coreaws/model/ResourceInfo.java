package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResourceInfo implements Serializable {

    private static final long serialVersionUID = 578103847205870626L;
    private int running;
    private int stop;
    private int etc;
    private int networks;
    private int securityGroups;
    private int publicIps;
    private int users;
    private int subnets;
    private int images;
    private int keyPairs;
    private int volumes;
    private int snapshots;
    private int diskUsage;
    private int storageUsage;
    private int databaseUsage;
    private int databaseCount;
    private int storageCount;
    private int loadBalancer;

    public int getServers () {
        return running + stop + etc;
    }
}
