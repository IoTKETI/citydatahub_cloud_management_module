package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Volume Model
 */
@Data
public class Snapshots implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String status;
    @Key private String progress;
    @Key private String description;
    @Key private String created_at;
    @Key private Object metadata;
    @Key private String volume_id;
    @Key private String project_id;
    @Key private Integer size;
    @Key private String id;
    @Key private String name;
}
