package com.datahub.infra.core.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;

    private ErrorResponse(ErrorCode code, Exception e) {
        this.timestamp = new Timestamp(System.currentTimeMillis()).toString();
        this.status = code.getStatus();
        this.error = code.name();
        this.message = e.getMessage();
    }

    private ErrorResponse(final ErrorCode code, String message) {
        this.timestamp = new Timestamp(System.currentTimeMillis()).toString();
        this.status = code.getStatus();
        this.error = code.name();
        this.message = message;
    }

    private ErrorResponse(final ErrorCode code, String time, String message) {
        this.timestamp = time;
        this.status = code.getStatus();
        this.error = code.name();
        this.message = message;
    }

    public ErrorResponse(String timestamp, int status, String error, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public static ErrorResponse of(final ErrorCode code, Exception e) {
        return new ErrorResponse(code, e);
    }

    public static ErrorResponse of(final ErrorCode code, String message) {
        return new ErrorResponse(code, message);
    }

    public static ErrorResponse of(final ErrorCode code, String time, String message) {
        return new ErrorResponse(code, time, message);
    }

    public static ErrorResponse of(String timestamp, int status, String error, String message) {
        return new ErrorResponse(timestamp, status, error, message);
    }
}