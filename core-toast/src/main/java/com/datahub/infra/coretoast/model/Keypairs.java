package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Header Model
 */
@Data
public class Keypairs implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String public_key;
    @Key private String user_id;
    @Key private String name;
    @Key private Boolean deleted;
    @Key private String created_at;
    @Key private String updated_at;
    @Key private String fingerprint;
    @Key private String deleted_at;
    @Key private Integer id;
}
