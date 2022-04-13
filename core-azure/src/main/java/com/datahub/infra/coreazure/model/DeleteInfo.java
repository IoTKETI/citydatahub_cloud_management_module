package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteInfo implements Serializable {
    private static final long serialVersionUID = -4137673447100054200L;
    private String id;
    private String name;

    public DeleteInfo() {

    }
}
