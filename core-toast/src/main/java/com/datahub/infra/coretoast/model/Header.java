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
public class Header implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String resultMessage;
    @Key private Boolean isSuccessful;
    @Key private Integer resultCode;

    @Override
    public String toString() {
        return "header{" +
                "resultMessage=" + resultMessage + '\'' +
                ", isSuccessful=" + isSuccessful + '\'' +
                ", resultCode=" + resultCode +
                '}';
    }
}
