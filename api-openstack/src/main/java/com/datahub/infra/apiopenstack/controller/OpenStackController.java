package com.datahub.infra.apiopenstack.controller;

import com.datahub.infra.apiopenstack.service.OpenStackService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.core.util.ObjectSerializer;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import com.datahub.infra.coreopenstack.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONObject;
import org.openstack4j.model.storage.block.VolumeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cloudServices/openstack")
public class OpenStackController {
    private static Logger logger = LoggerFactory.getLogger(OpenStackController.class);

    @Autowired
    private OpenStackService openStackService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private AES256Util aes256Util;

    @RequestMapping(value = {"","/"}, method = RequestMethod.GET)
    @ResponseBody
    public List<CredentialInfo> getCredentialOpenstack(@RequestHeader(value = "credential") String credential) {

        String type = "openstack";
        return openStackService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }

    @RequestMapping(value = "/servers", method = RequestMethod.GET)
    @ResponseBody
    public List<ServerInfo> getServers(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String serverState,
            @RequestParam(required = false) Boolean webCheck,
            @ModelAttribute ServerModel serverModel
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));


        Object json = "";
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            json = mapper.writeValueAsString(serverModel);
        }catch (JsonProcessingException e){
            e.printStackTrace();
        }

        if(webCheck == null) {
            webCheck = false;
        }

        if(name != null){
            return openStackService.getServers_Search(credentialInfo, project, name, "name");
        }else if (serverState != null){
            return openStackService.getServers_Search(credentialInfo, project, serverState, "serverState");
        }
        else if (json != ""){
            return openStackService.getServers_model(credentialInfo, project, webCheck, "[" + json + "]");
        }

        return openStackService.getServers(credentialInfo, project, webCheck);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.GET)
    @ResponseBody
    public List<ServerInfo> getServers(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String id,
            @RequestParam(required = false) Boolean webCheck
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck == null) {
            webCheck = false;
        }

        return openStackService.getServer(credentialInfo, project, id, webCheck);
    }

    @RequestMapping(value = "/volumes", method = RequestMethod.GET)
    @ResponseBody
    public List<VolumeInfo> getVolumes(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(defaultValue = "false") Boolean bootable,
            @RequestParam(defaultValue = "false") Boolean available,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String volumeState,
            @RequestParam(required = false) Boolean webCheck
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck == null) {
            webCheck = false;
        }

        if(name != null){
            return openStackService.getVolumes_Search(credentialInfo, project, bootable, available, name, "name");
        }else if (volumeState != null){
            return openStackService.getVolumes_Search(credentialInfo, project, bootable, available, volumeState, "volumeState");
        }

        return openStackService.getVolumes(credentialInfo, project, bootable, available, webCheck);
    }

    @RequestMapping(value = "/volumeTypes", method = RequestMethod.GET)
    @ResponseBody
    public List<? extends VolumeType> getVolumeTypes(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.getVolumeTypes(credentialInfo);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes", method = RequestMethod.POST)
    public @ResponseBody
    Object createVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody CreateVolumeInfo createVolumeInfo
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if(webCheck==null){
            webCheck=false;
        }

        return openStackService.createVolume(credentialInfo, project, createVolumeInfo, webCheck);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public List<VolumeInfo> getVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String volumeId,
            @RequestParam(required = false) Boolean webCheck
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck == null) {
            webCheck = false;
        }

        return openStackService.getVolume(credentialInfo, project, volumeId, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes/{id}/delete", method = RequestMethod.POST)
    public @ResponseBody DeleteInfo deleteVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String volumeId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        return openStackService.deleteVolume(credentialInfo, project, volumeId);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.DELETE)
    public @ResponseBody DeleteInfo deleteVolume2(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String volumeId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.deleteVolume(credentialInfo, project, volumeId);
    }

    @RequestMapping(value = "/allImages", method = RequestMethod.GET)
    @ResponseBody
    public List<NovaImageInfo> getAllImages(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(defaultValue = "false") Boolean active
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.getAllImages(credentialInfo, project);
    }

    @RequestMapping(value = "/flavors", method = RequestMethod.GET)
    @ResponseBody
    public List<FlavorInfo> getFlavors(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.getFlavors(credentialInfo, project);
    }

    @RequestMapping(value = "/networks", method = RequestMethod.GET)
    @ResponseBody
    public List<NetworkInfo> getNetworks(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String networkState,
            @RequestParam(required = false) Boolean webCheck
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck == null) {
            webCheck = false;
        }

        if(name != null){
            return openStackService.getNetworks_Search(credentialInfo, project, name, "name");
        }else if (networkState != null){
            return openStackService.getNetworks_Search(credentialInfo, project, networkState, "networkState");
        }

        return openStackService.getNetworks(credentialInfo, project, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks", method = RequestMethod.POST)
    public @ResponseBody
    Object createNetwork(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody CreateNetworkInfo createNetworkInfo
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        if(webCheck==null){
            webCheck=false;
        }

        return openStackService.createNetwork(credentialInfo, project, createNetworkInfo, webCheck);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks/{id}/delete", method = RequestMethod.POST)
    public @ResponseBody DeleteInfo deleteNetwork(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String networkId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.deleteNetwork(credentialInfo, project, networkId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/networks/{id}", method = RequestMethod.DELETE)
    public @ResponseBody DeleteInfo deleteNetwork2(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String networkId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.deleteNetwork(credentialInfo, project, networkId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers", method = RequestMethod.POST)
    public @ResponseBody
    Object createServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Boolean webCheck,
            @RequestBody CreateServerInfo createServerInfo
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        if (webCheck==null){
            webCheck= false;
        }
        return openStackService.createServer(credentialInfo, project, createServerInfo, webCheck);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.DELETE)
    public @ResponseBody DeleteInfo deleteServer2(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.delete(credentialInfo, project, serverId);
    }

    @RequestMapping(value = "/images", method = RequestMethod.GET)
    @ResponseBody
    public List<ImageInfo> getImages(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) String project,
            @RequestParam(defaultValue = "false") Boolean active,
            @ModelAttribute ImageModel imageModel
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        Object json = "";
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            json = mapper.writeValueAsString(imageModel);
        }catch (JsonProcessingException e){
            e.printStackTrace();
        }

        if (json != ""){
            return openStackService.getImages(credentialInfo, project, active, "[" + json + "]");
        }

        return openStackService.getImages(credentialInfo, project, active, null);
    }

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    @ResponseBody
    public List<ProjectInfo> getProjects(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.getProjects(credentialInfo);
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.GET)
    @ResponseBody
    public ProjectInfo getProjects(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "projectId") String project
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return openStackService.getProject(credentialInfo, project);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteCredential(@PathVariable(required = true) String id,
                                   @RequestParam(required = false) String project,
                                   @RequestHeader(value = "credential") String credential) {

        String type = "openstack";

        Map<String, Object> params = new HashMap<>();
        params.put("type", id);

        CredentialInfo credentialInfo = credentialService.getCredentialInfo(params);

        JSONObject test = new JSONObject();
        test.put("id", credentialInfo.getType());

        openStackService.deleteCredential(credentialInfo, project, id, credentialDao);

        return null;
    }
}