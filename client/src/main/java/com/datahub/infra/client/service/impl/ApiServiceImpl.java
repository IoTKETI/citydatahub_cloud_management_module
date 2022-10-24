package com.datahub.infra.client.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datahub.infra.client.service.ApiService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.coredb.service.CredentialService;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiServiceImpl implements ApiService {
    private final static Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);


    @Value("${apigateway_local.url}")
    private String apiUrl;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AES256Util aes256Util;

    @Override
    public List<CredentialInfo> getCredentialsInfo(List<CredentialInfo> list) {
        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo credentialInfo = new CredentialInfo();
            credentialInfo.setId(list.get(i).getType());
            credentialInfo.setName(list.get(i).getName());
            credentialInfo.setCspType((list.get(i).getType().equals("aws")) ? "1" : list.get(i).getType().equals("azure") ? "2" : "3");
            credentialInfo.setDomain(list.get(i).getDomain());
            credentialInfo.setUrl(list.get(i).getUrl());
            credentialInfo.setTenantId(list.get(i).getTenant());
            credentialInfo.setAccessId(list.get(i).getAccessId());
            credentialInfo.setAccessToken(list.get(i).getAccessToken());
            credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
            credentialInfo.setProjects(list.get(i).getProjects());
            credentialInfo.setCloudType(list.get(i).getCloudType());
            open.add(credentialInfo);
        }

        try {
            jsonString = mapper.writeValueAsString(open);
        } catch(IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);

        return jsonArray;
    }

    @Override
    public List<CredentialInfo> getCredentialsInfo_Search(List<CredentialInfo> list, String value, String type) {
        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo credentialInfo = new CredentialInfo();
            credentialInfo.setId(list.get(i).getType());
            credentialInfo.setName(list.get(i).getName());
            credentialInfo.setCspType((list.get(i).getType().equals("aws")) ? "1" : list.get(i).getType().equals("azure") ? "2" : "3");
            credentialInfo.setDomain(list.get(i).getDomain());
            credentialInfo.setUrl(list.get(i).getUrl());
            credentialInfo.setTenantId(list.get(i).getTenant());
            credentialInfo.setAccessId(list.get(i).getAccessId());
            credentialInfo.setAccessToken(list.get(i).getAccessToken());
            credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
            credentialInfo.setProjects(list.get(i).getProjects());
            credentialInfo.setCloudType(list.get(i).getCloudType());
            if(type.equals("name")){
                if(credentialInfo.getName().equals(value)) open.add(credentialInfo);
            }
            if(type.equals("cspType")){
                if(credentialInfo.getCspType().equals(value)) open.add(credentialInfo);
            }
            if(type.equals("cloudType")){
                if(credentialInfo.getCloudType().equals(value)) open.add(credentialInfo);
            }
        }

        try {
            jsonString = mapper.writeValueAsString(open);
        } catch(IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);

//            return apiService.getCredentialsInfo(credentialService.getCredentials(new HashMap<>()));
        logger.error("-------- jsonArray -------- ==== " + jsonArray);
        return jsonArray;
    }

    @Override
    public List<CredentialInfo> getCredential(List<CredentialInfo> list, String type) {

        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List <CredentialInfo> open = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CredentialInfo credentialInfo = new CredentialInfo();
            if(list.get(i).getType().equals(type)){
                credentialInfo.setId(list.get(i).getType());
                credentialInfo.setName(list.get(i).getName());
                credentialInfo.setCspType((list.get(i).getType().equals("aws")) ? "1" : list.get(i).getType().equals("azure") ? "2" : "3");
                credentialInfo.setDomain(list.get(i).getDomain());
                credentialInfo.setUrl(list.get(i).getUrl());
                credentialInfo.setTenantId(list.get(i).getTenant());
                credentialInfo.setAccessId(list.get(i).getAccessId());
                credentialInfo.setAccessToken(list.get(i).getAccessToken());
                credentialInfo.setCreatedAt(list.get(i).getCreatedAt());
                credentialInfo.setProjects(list.get(i).getProjects());
                credentialInfo.setCloudType(list.get(i).getCloudType());
                open.add(credentialInfo);
            }
        }

        try {
            jsonString = mapper.writeValueAsString(open);
        } catch(IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.fromObject(jsonString);

        return jsonArray;
    }

    @Override
    public boolean getCredentialsCheck(List<CredentialInfo> list, String type){
        if(type.equals("openstack")){
            return true;
        }else{
            for(CredentialInfo info : list){
                if(info.getType().equals(type)){
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean getCredentialsNameCheck(List<CredentialInfo> list, String name){
        for(CredentialInfo info : list){
            if(info.getName().equals(name)){
                return false;
            }
        }
        return true;
    }

    public String getCloudType(CredentialInfo createData){
        String tempType = createData.getType();
        String type;
        if(tempType.equals("openstack")) {
            type = "private";
        }else{
            type = "public";
        }
        return type;
    }
}
