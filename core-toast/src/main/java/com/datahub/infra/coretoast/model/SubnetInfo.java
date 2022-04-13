package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yby654
 * @date 2020.6.2
 * @brief TOAST SubnetInfo Model
 */
@Data
public class SubnetInfo implements Serializable {
    private static final long serialVersionUID = 3729901621928159650L;

    @Key
    private Subnets[] subnets;

    public SubnetInfo(){}
}