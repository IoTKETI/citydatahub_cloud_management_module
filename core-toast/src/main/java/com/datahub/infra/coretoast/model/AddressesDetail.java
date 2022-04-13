package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class AddressesDetail implements Serializable {
    private static final long serialVersionUID = 1043142547999634643L;
    @Key private Integer version;
    @Key private String addr;
}
