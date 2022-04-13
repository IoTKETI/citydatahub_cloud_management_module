package com.datahub.infra.coretoast.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ksg
 * @date 2020.6.5
 * @brief Toast Resource 모델
 */

@Data
public class ResourceInfo implements Serializable {
    private static final long serialVersionUID = -5277975201959955993L;

    private int running;
    private int stop;
    private int etc;
    private int total;

    private int users;

    private int networks;
    private int securityGroups;
    private int publicIp;

    private int volumes;
    private int volumeUsage;
    private int snapshots;

//    value 계속 추가할 것

}
