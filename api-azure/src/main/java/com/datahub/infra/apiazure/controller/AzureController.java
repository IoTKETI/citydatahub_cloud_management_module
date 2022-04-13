package com.datahub.infra.apiazure.controller;

import com.datahub.infra.apiazure.service.AzureService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.core.util.ObjectSerializer;
import com.datahub.infra.coreazure.model.DeleteInfo;
import com.datahub.infra.coreazure.model.DiskInfo;
import com.datahub.infra.coreazure.model.NetworkInfo;
import com.datahub.infra.coreazure.model.ServerInfo;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/cloudServices/azure")
public class AzureController {

    private static Logger logger = LoggerFactory.getLogger(AzureController.class);

    @Autowired
    private AzureService azureService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private AES256Util aes256Util;

    @RequestMapping(value = {"","/"}, method = RequestMethod.GET)
    @ResponseBody
    public List<CredentialInfo> getCredentialAzure(@RequestHeader(value = "credential") String credential) {

        String type = "azure";
        return azureService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }

    @RequestMapping(value = "/servers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ServerInfo> getServers(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String serverState,
            @RequestParam(required = false) Boolean webCheck
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        if(name != null){
            return azureService.getServers_Search(credentialInfo, name, "name");
        }else if (serverState != null) {
            return azureService.getServers_Search(credentialInfo, serverState, "serverState");
        }

        return azureService.getServers(credentialInfo, webCheck);
    }


    @RequestMapping(value = "/servers/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ServerInfo> getServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable("id") String serverId
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        if(webCheck == null) {
            webCheck = false;
        }
        return azureService.getServer(credentialInfo, serverId, webCheck);
    }

    @RequestMapping(value = "/volumes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<DiskInfo> getDisks(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String volumeState,
            @RequestParam(required = false) Boolean webCheck
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck==null){
            webCheck=false;
        }

        if(name != null){
            return azureService.getDisks_Search(credentialInfo, name, "name");
        }else if (volumeState != null) {
            return azureService.getDisks_Search(credentialInfo, volumeState, "volumeState");
        }

        return azureService.getDisks(credentialInfo, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object createDisk(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody Map<String, Object> createData
    ){

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck==null){
            webCheck=false;
        }

        return azureService.createDisk(credentialInfo, createData, webCheck);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public DeleteInfo deleteDisk(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable("id") String volumeId
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        volumeId = new String(java.util.Base64.getDecoder().decode(volumeId));

        if(webCheck==null){
            webCheck=false;
        }

        return azureService.deleteDisk(credentialInfo, volumeId, webCheck);
    }



    @RequestMapping(value = "/volumes/{subscriptions}/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<DiskInfo> getDisk(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }
        if(webCheck==null){
            webCheck=false;
        } else if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }

        return azureService.getDisk(credentialInfo, moduleName, webCheck);
    }

    @RequestMapping(value = "/volumes/{subscriptions}/**", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public DiskInfo deleteDisk_test(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }

        if(webCheck==null){
            webCheck=false;
        }
        if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }

        return azureService.deleteDisk_test(credentialInfo, moduleName);
    }

    @RequestMapping(value = "/networks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<NetworkInfo> getNetworks(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "resourceGroup", required = false) String resourceGroup,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String networkState,
            @RequestParam(required = false) Boolean webCheck
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        Map<String, Object> params = new HashMap<>();
        if(webCheck==null){
            webCheck=false;
        }

        if(name != null){
            return azureService.getNetworks_Search(credentialInfo, resourceGroup, region, name, "name");
        }else if (networkState != null) {
            return azureService.getNetworks_Search(credentialInfo, resourceGroup, region, networkState, "networkState");
        }

        return azureService.getNetworks(credentialInfo, resourceGroup, region, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object getNetworks_Create_test(
            @RequestHeader(value = "credential") String credential,
            @RequestBody Map<String, Object> createData,
            @RequestParam(required = false) Boolean webCheck
    ){

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck==null){
            webCheck=false;
        }

        return azureService.getNetworks_Create_test(credentialInfo, createData, webCheck);
    }

    @RequestMapping(value = "/networks/{subscriptions}/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<NetworkInfo> getNetworks_Detail_azure(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "resourceGroup", required = false) String resourceGroup,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }
        if(webCheck==null){
            webCheck=false;
        } else if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }

        return azureService.getNetworks_Detail_azure(credentialInfo, resourceGroup, region, subscriptions, request, moduleName, webCheck);
    }

    @RequestMapping(value = "/networks/{subscriptions}/**", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public DeleteInfo deleteNetwork(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }

        if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }

        return azureService.deleteNetwork(credentialInfo, moduleName, webCheck);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public void deleteServer2(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        if(webCheck==null) {
            webCheck = false;
        }

        azureService.delete(credentialInfo, serverId, webCheck);
    }

    @RequestMapping(value = "/servers/{subscriptions}/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ServerInfo> getServer_detail(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "resourceGroup", required = false) String resourceGroup,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }
        if(webCheck==null){
            webCheck = false;
        }
        if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }


        if(webCheck){
            return azureService.getServer_detail(credentialInfo, moduleName, webCheck);
        } else{
            return azureService.getServers(credentialInfo, moduleName, webCheck);
        }

    }

    @RequestMapping(value = "/servers/{subscriptions}/**", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public DeleteInfo deleteServer_test(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable String subscriptions,
            HttpServletRequest request
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String moduleName;
        if (null != arguments && !arguments.isEmpty()) {
            moduleName = subscriptions + '/' + arguments;
        } else {
            moduleName = subscriptions;
        }
        if(webCheck==null){
            webCheck = false;
        }
        if(!webCheck) {
            byte[] decodeBytes = java.util.Base64.getDecoder().decode(moduleName);
            moduleName = new String(decodeBytes);
        }
        return azureService.deleteServer_test(credentialInfo, moduleName);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    Object createServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody Map<String, Object> createData
    ){

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck==null){
            webCheck = false;
        }

        return azureService.createServer(credentialInfo, createData, webCheck);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Object deleteCredential(@PathVariable(required = true) String id,
                                   @RequestParam(required = false) String project,
                                   @RequestHeader(value = "credential") String credential) {

        String type = "azure";

        Map<String, Object> params = new HashMap<>();
        params.put("type", id);

        CredentialInfo credentialInfo = credentialService.getCredentialInfo(params);

        JSONObject test = new JSONObject();
        test.put("id", credentialInfo.getType());

        azureService.deleteCredential(credentialInfo, project, id, credentialDao);

        return null;
    }

}
