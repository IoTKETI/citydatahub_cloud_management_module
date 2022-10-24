package com.datahub.infra.client.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datahub.infra.client.exception.ValidationFailException;
import com.datahub.infra.client.service.ApiService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.model.CredentialInfo2;
import com.datahub.infra.core.model.UserInfo;
import com.datahub.infra.coredb.service.CredentialService;
import com.microsoft.azure.management.graphrbac.Credential;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class CloudServiceController {
    private static Logger logger = LoggerFactory.getLogger(CloudServiceController.class);

    @Autowired
    private ApiService apiService;

    @Autowired
    private CredentialService credentialService;

//    @Secured("ROLE_ADMIN")
    @RequestMapping(value = "/cloudServices", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<CredentialInfo> getCredentialsInfo(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cspType,
            @RequestParam(required = false) String cloudType,
            HttpSession session
    ) {

        if(session != null) {
            if(name != null){
                return apiService.getCredentialsInfo_Search(credentialService.getCredentials(new HashMap<>()), name, "name");
            }else if(cspType != null){
                return apiService.getCredentialsInfo_Search(credentialService.getCredentials(new HashMap<>()), cspType, "cspType");
            }else if(cloudType != null){
                return apiService.getCredentialsInfo_Search(credentialService.getCredentials(new HashMap<>()), cloudType, "cloudType");
            }
            return apiService.getCredentialsInfo(credentialService.getCredentials(new HashMap<>()));
        }
        return null;
    }

    @RequestMapping(value = "/cloudServices/{type}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<CredentialInfo> getCredentialAll(
            @RequestHeader(value = "credential") String credential, @PathVariable(value = "type") String type
    ) {

        return apiService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/cloudServices", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object createCredential(
            @RequestBody CredentialInfo2 createData2,
            HttpSession session
    ) {

        CredentialInfo createData = new CredentialInfo(createData2);

        List<CredentialInfo> credential = credentialService.getCredentials(new HashMap<>());

        boolean isChecked = apiService.getCredentialsCheck(credential, createData.getType());
        boolean isChecked2 = apiService.getCredentialsNameCheck(credential, createData.getName());

        if(isChecked && isChecked2){
            createData.setCloudType(apiService.getCloudType(createData));
            boolean isValid = true;
            if(isValid) {
                String jsonString = null;
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                CredentialInfo result = credentialService.createCredentialApi(createData);

                try {
                    jsonString = mapper.writeValueAsString(result);
                } catch(IOException e) {
                    e.printStackTrace();
                }
                JSONObject jsonobj = JSONObject.fromObject(jsonString);

                return jsonobj;
            } else {
                throw new ValidationFailException("유효하지 않은 credential 정보 입니다.");
            }
        }else{
            throw new ValidationFailException("다른 credential 계정이 존재합니다.");
        }
    }
}
