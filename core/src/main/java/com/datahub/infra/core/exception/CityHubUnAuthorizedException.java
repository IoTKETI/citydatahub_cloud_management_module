package com.datahub.infra.core.exception;

import org.springframework.core.NestedRuntimeException;

public class CityHubUnAuthorizedException extends NestedRuntimeException {
    private String title = "";
    private String detail = "";

    public CityHubUnAuthorizedException(String msg) {
        super(msg);
        this.title = msg;
        this.detail = msg;
    }

    public CityHubUnAuthorizedException(String title, String detail) {
        super(title);
        this.title = title;
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }
}
