package com.datahub.infra.apiazure.controller;

import com.datahub.infra.apiazure.service.AzureService;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.model.ImageDetailInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.core.util.ObjectSerializer;
import com.datahub.infra.coreazure.model.*;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import com.datahub.infra.coredb.service.ImageService;
import com.microsoft.azure.management.compute.VirtualMachineImage;
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
import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/cloudServices/azure")
public class AzureController {

    private static Logger logger = LoggerFactory.getLogger(AzureController.class);

    @Autowired
    private AzureService azureService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private AES256Util aes256Util;

    @RequestMapping(value = {"","/"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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

    @RequestMapping(value = "/images", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ImageInfo> getImages(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return  azureService.getImages(credentialInfo);
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

        // /All Path
        final String path =
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        // /** Path
        final String bestMatchingPattern =
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();

        // arguments = path - bestMatchingPattern
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
//        System.out.println("############# = " + moduleName);
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

    @RequestMapping(value = "/isusableipchk", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, String> getIsUsableIpchk (
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "network") String network,
            @RequestParam(value = "privateIp") String privateIp
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

//        Map<String, Object> params = new HashMap<>();
//        params.put("network", network);
//        params.put("privateIp", privateIp);

        return azureService.getIsUsableIp(credentialInfo, network, privateIp);

    }

    @RequestMapping(value = "/publicips", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<PublicIpInfo> getPublicIps(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "resourceGroup", required = false) String resourceGroup,
            @RequestParam(value = "region", required = false) String region
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getPublicIps(credentialInfo, resourceGroup, region);
    }

    @RequestMapping(value = "/loadbalancers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<LoadBalancerInfo> getLoadBalancers(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return  azureService.getLoadBalancers(credentialInfo);
    }

    @RequestMapping(value = "/securitygroups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<SecurityGroupInfo> getSecurityGroups(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getSecurityGroups(credentialInfo);
    }

    @RequestMapping(value = "/activedirectorygroups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ActiveDirectoryGroupInfo> getActiveDirectoryGroups(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getActiveDirectoryGroups(credentialInfo);
    }

    @RequestMapping(value = "/activedirectoryusers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ActiveDirectoryUserInfo> getActiveDirectoryUsers(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getActiveDirectoryUsers(credentialInfo);
    }

    @RequestMapping(value = "/storageaccounts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<StorageAccountInfo> getStorageAccounts(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getStorageAccounts(credentialInfo);
    }

    @RequestMapping(value = "/genericresources", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<GenericResourceInfo> getGenericResources(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getGenericResources(credentialInfo);
    }

    @RequestMapping(value = "/genericresourcessummary", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Integer> getGenericResourcesSummary(
            @RequestHeader(value = "credential") String credential) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        Map<String, Integer> list = azureService.getGenericResourcesSummary(credentialInfo);

        return list;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers/{id}/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    ServerInfo startServer(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        return azureService.start(credentialInfo, serverId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers/{id}/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    ServerInfo stopServer(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        return azureService.stop(credentialInfo, serverId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers/{id}/reboot", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody
    ServerInfo rebootServer(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        return azureService.reboot(credentialInfo, serverId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers/{id}/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public void deleteServer(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(required = false) Boolean webCheck,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        serverId = new String(Base64.getDecoder().decode(serverId));

        if(webCheck==null){
            webCheck=false;
        }

        azureService.delete(credentialInfo, serverId, webCheck);
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

    @RequestMapping(value = "/servers/{id}/metric", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Object> getServerMetric(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String id,
            @RequestParam(value = "endDate") String endDate,
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "InstanceId", required = false, defaultValue = "InstanceId") String name,
            @RequestParam(value = "metricName") String metricName,
            @RequestParam(value = "interval") Integer interval,
            @RequestParam(value = "statistic") String statistic
    ){
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        RequestMetricInfo requestMetricInfo = new RequestMetricInfo();
        requestMetricInfo.setId(new String(Base64.getDecoder().decode(id)));
        requestMetricInfo.setEndDate(new Date(Long.parseLong(endDate)));
        requestMetricInfo.setStartDate(new Date(Long.parseLong(startDate)));
        requestMetricInfo.setName(name);
        requestMetricInfo.setMetricName(metricName);
        requestMetricInfo.setInterval(interval);
        requestMetricInfo.setStatistic(statistic);

        return azureService.getServerMetric(credentialInfo, requestMetricInfo);
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

    @RequestMapping(value = "/resourcegroups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ResourceGroupInfo> getResourceGroup(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "region", required = false) String region
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        //if(region != null) credentialInfo.setRegion(region);

        return azureService.getResourceGroups(credentialInfo, region);
    }

    @RequestMapping(value = "/sizes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<SizeInfo> getSizes(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "region", required = false) String region
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getSizes(credentialInfo, region);
    }

    @RequestMapping(value = "/dashboarddata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Map> getDashboard(
            @RequestHeader(value = "credential") String credential
    ){

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getDashboard(credentialInfo);
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

    @RequestMapping(value = "/subscriptions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<SubscriptionInfo> getSubscriptions(
            @RequestHeader(value = "credential") String credential
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getSubscriptions(credentialInfo);
    }

    @RequestMapping(value = "/regions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<RegionInfo> getRegions(
            @RequestHeader(value = "credential") String credential
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getRegions(credentialInfo);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Boolean> checkValidate(
            @RequestHeader(value = "credential") String credential
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return new HashMap<String, Boolean> (){{
            put("result", azureService.validateCredential(credentialInfo));
        }};
    }

    @RequestMapping(value = "/resource", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResourceInfo getResource(
            @RequestHeader(value = "credential") String credential
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getResourceUsage(credentialInfo);
    }

    @RequestMapping(value = "/imagedetail/{imageId}", method = RequestMethod.GET)
    @ResponseBody
    public VirtualMachineImage getImageDetail(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "imageId") String imageId,
            @RequestParam(value = "region") String region
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return azureService.getImageDetail(credentialInfo, region, imageId);
    }

    @RequestMapping(value = "/publicimages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public List<ImageDetailInfo> getPublicImages(
            @RequestHeader(value = "credential") String credential,
            @RequestParam(value = "location") String location
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return imageService.getImageDetails("azure", location);
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
