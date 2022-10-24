package com.datahub.infra.coredb.service.impl;

import com.datahub.infra.core.Constants;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.ActionService;
import com.datahub.infra.coredb.service.CredentialService;
import fi.evident.dalesbred.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CredentialServiceImpl implements CredentialService {
    private final static Logger logger = LoggerFactory.getLogger(CredentialServiceImpl.class);

    private static List<CredentialInfo> credentialInfos = new ArrayList<>();

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private ActionService actionService;

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

    @Override
    public CredentialInfo createCredentialApi(CredentialInfo info) {
        info.setId(UUID.randomUUID().toString());

        int result = credentialDao.createCredential(info);

        String groupId = "";
        String getId = "admin";

        String actionId = actionService.initAction(groupId, getId, info.toString(), info.getId(),
                info.getName(), Constants.ACTION_CODE.CREDENTIAL_CREATE, Constants.HISTORY_TYPE.CREDENTIAL);

        if(result == 1) {

            updateCredentialsFromMemory();

            actionService.setActionResult(actionId, Constants.ACTION_RESULT.SUCCESS);

            return credentialDao.getCredentialInfo(new HashMap<String, Object>(){{
                put("id", info.getId());
            }});
        } else {
            actionService.setActionResult(actionId, Constants.ACTION_RESULT.FAILED);
        }

        info.setCreatedAt(new Timestamp(new Date().getTime()));

        return info;
    }
}
