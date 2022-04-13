package com.datahub.infra.core.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

    BAD_REQUEST(400, "s.exception-400", null),
    FORBIDDEN(403, "s.exception-403", null),
    INTERNAL_SERVER_ERROR(500, "s.exception-500", null),

    SERVICE_UNAVAILABLE(503, "s.service-unavailable", null),

    MALFORMEDURLEXCEPTION(400, "s.exception-malformed-url", null),
    FILENOTFOUNDEXCEPTION(400, "s.exception-file-not-found", null),
    RESOURCEACCESSEXCEPTION(408, "s.exception-timeout", null),
    CLIENTABORTEXCEPTION(408, "s.timeout-exception", null),

    CREDENTIAL_INVALID(400, "s.t.not-exist", new String[]{"w.credential"}),
    VM_CREATE_FAIL(400, "s.t.fail", new String[]{"w.create"}),
    VM_DELETE_FAIL(400, "s.t.fail", new String[]{"w.delete"}),
    VM_ACTION_FAIL(400, "s.t.fail", new String[]{"w.action"});

    private int status;
    private final String messageProperty;
    private final String[] messagePropertyPropsName;

    ErrorCode(final int status, String messageProperty, String[] messagePropertyPropsName) {
        this.status = status;
        this.messageProperty = messageProperty;
        this.messagePropertyPropsName = messagePropertyPropsName;
    }

    public int getStatus() {
        return status;
    }

    public String getMessageProperty() {
        return messageProperty;
    }

    public String[] getMessagePropertyPropsName() {
        return messagePropertyPropsName;
    }
}