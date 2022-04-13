package com.datahub.infra.coredb.dao;

import com.datahub.infra.core.model.CredentialInfo;

import java.util.List;
import java.util.Map;

public interface CredentialDao {
    List<CredentialInfo> getCredentials(Map<String, Object> params);

    int getTotal(Map<String, Object> params);

    CredentialInfo getCredentialInfo(Map<String, Object> params);

    int deleteCredential(CredentialInfo info);
}

