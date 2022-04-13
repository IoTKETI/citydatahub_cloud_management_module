package com.datahub.infra.coreopenstack.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteInfo implements Serializable {

    private static final long serialVersionUID = 8585354799376127134L;
    private String id;
    private String name;

    public DeleteInfo() {
    }

}
