package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Links implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String href;
    @Key private String rel;
}
