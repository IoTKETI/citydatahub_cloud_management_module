package com.datahub.infra.core.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiErrorResponse {

    private Timestamp timestamp;
    private int status;
    private String error;
    private String message;
    private String exception;
    private Object target;

    private ApiErrorResponse(final ErrorCode code, Exception e) {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.status = code.getStatus();
        this.error = code.name();
        this.exception = e.getClass().getName();
        this.message = e.getMessage();
        if (e instanceof ApiException) {
            if (((ApiException) e).getTarget() != null) {
                target = ((ApiException) e).getTarget();
            }
        }
    }

    private ApiErrorResponse(ErrorCode code, Exception e, String message) {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.status = code.getStatus();
        this.error = code.name();
        this.exception = e.getClass().getName();
        this.message = message;
    }

    public static ApiErrorResponse of(final ErrorCode code, Exception e) {
        return new ApiErrorResponse(code, e);
    }

    public static ApiErrorResponse of(final ErrorCode code, final Exception e, final String message) {
        return new ApiErrorResponse(code, e, message);
    }
}