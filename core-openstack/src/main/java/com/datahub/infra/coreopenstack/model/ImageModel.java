package com.datahub.infra.coreopenstack.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImageModel implements Serializable {

    private static final long serialVersionUID = 2897465774550361154L;
    private String server;
    private String name;
    private String status;
    private Integer minDisk;
    private Integer minRam;
    private String type;
    private Integer limit;
    private String marker;
}
