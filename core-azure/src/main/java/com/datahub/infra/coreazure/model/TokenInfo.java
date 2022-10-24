package com.datahub.infra.coreazure.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class TokenInfo implements Serializable {
    private static final long serialVersionUID = 8418260770788410876L;
    private String tokenType;
    private String accessToken;
    private String resource;

    public TokenInfo() {

    }

    public TokenInfo(Map<String, Object> params) {
        this.tokenType = (String) params.get("token_type");
        this.accessToken = (String) params.get("access_token");
        this.resource = (String) params.get("resource");
    }

    public String getHeaderInfo () { return  tokenType + " " + accessToken; }
}
