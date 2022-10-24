package com.datahub.infra.coredb.service;

import com.datahub.infra.core.model.CredentialInfo;

import java.util.List;
import java.util.Map;

public interface CredentialService {

    public CredentialInfo getCredentialsFromMemoryById(String credentialId);
    public List<CredentialInfo> getCredentialsFromMemory();
    public void updateCredentialsFromMemory();
    List<CredentialInfo> getCredentials(Map<String, Object> params);
    int getTotal(Map<String, Object> params);
    CredentialInfo getCredentialInfo(Map<String, Object> params);
    public CredentialInfo createCredentialApi(CredentialInfo info);

}
