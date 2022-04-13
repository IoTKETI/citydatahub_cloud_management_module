package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class Allocation_pools implements Serializable {
    private static final long serialVersionUID = 1043142547999634643L;
    @Key private String start;
    @Key private String end;
}
