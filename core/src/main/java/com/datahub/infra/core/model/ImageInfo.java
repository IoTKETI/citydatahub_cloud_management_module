package com.datahub.infra.core.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImageInfo implements Serializable {
    private static final long serialVersionUID = -4732500528249202654L;
    private String id;
    private String type;
}
