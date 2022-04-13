package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Header Model
 */
@Data
public class Image implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String id;
    @Key private List<Links> links;
}
