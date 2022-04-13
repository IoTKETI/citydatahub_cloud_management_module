package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordCredentials implements Serializable {
    private static final long serialVersionUID = -2199834068439467064L;
    @Key private String username;
    @Key private String password;

}
