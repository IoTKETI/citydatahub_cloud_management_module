package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author consine2c
 * @date 2020.6.1
 * @brief TOAST ServerInfo Model
 */
@Data
public class ServersInfo implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

//    @Key private Header header;
    @Key private Servers[] servers;

    public ServersInfo(){}

}
