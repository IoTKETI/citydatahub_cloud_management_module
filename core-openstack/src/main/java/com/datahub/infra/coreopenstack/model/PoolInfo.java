package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.network.Pool;

import java.io.Serializable;

@Data
public class PoolInfo implements Serializable {

    private static final long serialVersionUID = 6423963740882789376L;
    private String start;
    private String end;

    public PoolInfo() {

    }

    public PoolInfo(Pool info) {
        this.start = info.getStart();
        this.end = info.getEnd();
    }
}
