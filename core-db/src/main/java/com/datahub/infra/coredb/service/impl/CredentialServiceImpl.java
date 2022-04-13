package com.datahub.infra.coredb.service.impl;

import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import fi.evident.dalesbred.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CredentialServiceImpl implements CredentialService {
    private final static Logger logger = LoggerFactory.getLogger(CredentialServiceImpl.class);

    private static List<CredentialInfo> credentialInfos = new ArrayList<>();

    @Autowired
    private CredentialDao credentialDao;

    @Override
    public CredentialInfo getCredentialsFromMemoryById(String id) {
        if(id == null) return null;

        List<CredentialInfo> result = credentialInfos.stream().filter(credential -> credential.getId().equals(id)).collect(Collectors.toList());

        if(result.size() > 0) {
            return result.get(0);
        }
        return  null;
    }

    @Override
    public List<CredentialInfo> getCredentialsFromMemory() {
        if(credentialInfos.size() == 0) {
            updateCredentialsFromMemory();
        }
        return credentialInfos;
    }

    @Override
    public List<CredentialInfo> getCredentials(Map<String, Object> params) {
        List<CredentialInfo> list = credentialDao.getCredentials(params);

        return list;
    }

    @Override
    public void updateCredentialsFromMemory() {
        credentialInfos = getCredentials(new HashMap<String,Object>(){{
            put("sidx", "menu");
            put("sord", "asc");
        }});
    }

    @Override
    public int getTotal(Map<String, Object> params) {
        int totalCnt = credentialDao.getTotal(params);
        return totalCnt;
    }

    @Override
    public CredentialInfo getCredentialInfo(Map<String, Object> params) {
        CredentialInfo info = credentialDao.getCredentialInfo(params);

        return info;
    }

}
