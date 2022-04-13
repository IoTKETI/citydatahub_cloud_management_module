package com.datahub.infra.apiaws.controller;

import com.datahub.infra.apiaws.service.AwsService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.core.util.ObjectSerializer;
import com.datahub.infra.coreaws.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cloudServices/aws/")
public class AwsController {
    private static Logger logger = LoggerFactory.getLogger(AwsController.class);

    @Autowired
    private AwsService awsService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private AES256Util aes256Util;

    @RequestMapping(value = {"","/"}, method = RequestMethod.GET)
    @ResponseBody
    public List<CredentialInfo> getCredentialAWS(@RequestHeader(value = "credential") String credential) {
        String type = "aws";
        return awsService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteCredential(@PathVariable(required = true) String id,
                                   @RequestParam(required = false) String project,
                                   @RequestHeader(value = "credential") String credential) {
        String type = "aws";

        Map<String, Object> params = new HashMap<>();
        params.put("type", id);

        CredentialInfo credentialInfo = credentialService.getCredentialInfo(params);

        JSONObject test = new JSONObject();
        test.put("id", credentialInfo.getType());

        awsService.deleteCredential(credentialInfo, project, id, credentialDao);

        return null;
    }

    @RequestMapping(value = "/servers", method = RequestMethod.GET)
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

            return awsService.getServers_Search(credentialInfo, name, "name");
        }else if (serverState != null){
            return awsService.getServers_Search(credentialInfo, serverState, "serverState");
        }
        return awsService.getServers(credentialInfo, webCheck);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.GET)
    @ResponseBody
    public List<ServerInfo> getServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable("id") String id
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }
        return awsService.getServer(credentialInfo, id, webCheck);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.DELETE)
    public @ResponseBody Object deleteServer2(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        return awsService.delete(credentialInfo, serverId, webCheck);
    }

    @RequestMapping(value = "/volumes", method = RequestMethod.GET)
    @ResponseBody
    public List<VolumeInfo> getVolumes(
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
            return awsService.getVolumes_Search(credentialInfo, name, "name");
        }else if (volumeState != null) {
            return awsService.getVolumes_Search(credentialInfo, volumeState, "volumeState");
        }
        return awsService.getVolumes(credentialInfo, webCheck);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public List<VolumeInfo> getVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable("id") String id
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck==null){
            webCheck=false;
        }
        return awsService.getVolume(credentialInfo, id, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object deleteVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable("id") String volumeId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck==null){
            webCheck=false;
        }

        return awsService.deleteVolume(credentialInfo, volumeId, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks", method = RequestMethod.POST)
    public @ResponseBody
    Object createNetwork(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody Map<String, Object> data
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        return awsService.createNetwork(credentialInfo,data,webCheck);
    }

    @RequestMapping(value = "/networks", method = RequestMethod.GET)
    @ResponseBody
    public List<NetworkInfo> getNetworks(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String networkState,
            @RequestParam(required = false) Boolean webCheck
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }
        if(name != null){
            return awsService.getNetworks_Search(credentialInfo, name, "name");
        }else if (networkState != null) {
            return awsService.getNetworks_Search(credentialInfo, networkState, "networkState");
        }

        return awsService.getNetworks(credentialInfo, webCheck);
    }

    @RequestMapping(value = "/networks/{id}", method = RequestMethod.GET)
    @ResponseBody
    public List<NetworkInfo> getNetwork(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable(value = "id") String networkId
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }
        return awsService.getNetwork(credentialInfo,networkId,webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object delNetworks(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable(value = "id") String networkId
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        return awsService.deleteNetwork(credentialInfo,networkId,webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes", method = RequestMethod.POST)
    public @ResponseBody
    Object createVolumes(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody Map<String, Object> createData
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        return awsService.createVolume(credentialInfo, createData, webCheck);
    }


    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers", method = RequestMethod.POST)
    public @ResponseBody
    Object createServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody CreateServerInfo createServerInfo
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck == null) {
            webCheck = false;
        }

        return awsService.createServer(credentialInfo, createServerInfo, webCheck);
    }

    @RequestMapping(value = "/flavors", method = RequestMethod.GET)
    @ResponseBody
    public List<FlavorInfo> getFlavors(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "os") String os
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return awsService.getFlavors(credentialInfo, os);
    }

    @RequestMapping(value = "/images", method = RequestMethod.GET)
    @ResponseBody
    public List<ImageInfo> getImages(
            @RequestHeader(value = "credential") String credential
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return awsService.getImages(credentialInfo);
    }
}
