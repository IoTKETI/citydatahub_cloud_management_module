package com.datahub.infra.core.exception;

public class CredentialException extends ApiException {

    public CredentialException(Object target) {
        super(ErrorCode.CREDENTIAL_INVALID, "Credential Exception", target);
    }

    public CredentialException() {
        super(ErrorCode.CREDENTIAL_INVALID, "Credential Exception");
    }
}