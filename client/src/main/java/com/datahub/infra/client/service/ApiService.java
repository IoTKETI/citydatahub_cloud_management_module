package com.datahub.infra.client.service;

import com.datahub.infra.core.model.CredentialInfo;

import java.util.List;

public interface ApiService {

    boolean getCredentialsCheck(List<CredentialInfo> list, String type);

    boolean getCredentialsNameCheck(List<CredentialInfo> list, String name);

    String getCloudType(CredentialInfo createData);

    List<CredentialInfo> getCredentialsInfo(List<CredentialInfo> list);

    List<CredentialInfo> getCredentialsInfo_Search(List<CredentialInfo> list, String value, String type);

    List<CredentialInfo> getCredential(List<CredentialInfo> list, String type);

}
