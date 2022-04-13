package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteInfo implements Serializable {
    private static final long serialVersionUID = -8908819334506024014L;
    private String id;

    public DeleteInfo(){

    }
}
