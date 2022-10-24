package com.datahub.infra.apitoast.controller;

import com.datahub.infra.apitoast.service.ToastService;
import com.datahub.infra.coretoast.model.*;
import com.datahub.infra.core.model.CredentialInfo;
import com.datahub.infra.core.util.AES256Util;
import com.datahub.infra.core.util.ObjectSerializer;
import com.datahub.infra.coredb.dao.CredentialDao;
import com.datahub.infra.coredb.service.CredentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import net.minidev.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/infra/cloudServices/toast")
public class ToastController {
    private static Logger logger = LoggerFactory.getLogger(ToastController.class);

    @Autowired
    private ToastService toastService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AES256Util aes256Util;

    @Autowired
    private CredentialDao credentialDao;

    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)    @ResponseBody
    public List<CredentialInfo> getCredentialAWS(@RequestHeader(value = "credential") String credential) {

        String type = "toast";
        return toastService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public List<CredentialInfo> deleteCredential(@PathVariable(required = true) String id,
                                                 @RequestParam(required = false) String project,
                                                 @RequestHeader(value = "credential") String credential) {

        String type = "toast";

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        CredentialInfo credentialInfo = credentialService.getCredentialInfo(params);

        toastService.deleteCredential(credentialInfo, project, id, credentialDao);
        return toastService.getCredential(credentialService.getCredentials(new HashMap<>()), type);
    }

    @RequestMapping(value = "/servers", method = RequestMethod.GET)
    @ResponseBody
    public List<Servers> getServers(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getServers(credentialInfo);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ServerInfo getServer(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String id
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getServer(credentialInfo, id);
    }

    @RequestMapping(value = "/volumes", method = RequestMethod.GET)
    @ResponseBody
    public List<Volumes> getVolumes(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getVolumes(credentialInfo);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public VolumeInfo getVolume(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String volumeId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getVolume(credentialInfo, volumeId);
    }

    @RequestMapping(value = "/snapshots", method = RequestMethod.GET)
    @ResponseBody
    public List<Snapshots> getSnapshots(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getSnapshots(credentialInfo);
    }

    @RequestMapping(value = "/snapshots/{id}", method = RequestMethod.GET)
    @ResponseBody
    public SnapshotInfo getSnapshot(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String snapshotId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getSnapshot(credentialInfo, snapshotId);
    }

    @RequestMapping(value = "/resource", method = RequestMethod.GET)
    @ResponseBody
    public ResourceInfo getResource(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getResourceUsage(credentialInfo);
    }

    @RequestMapping(value = "/types", method = RequestMethod.GET)
    @ResponseBody
    public TypeInfo getVolumeTypes(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        return toastService.getVolumeTypes(credentialInfo);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/volumes", method = RequestMethod.POST)
    public @ResponseBody
    VolumeInfo createVolume(
            @RequestHeader(value = "credential") String credential,
            @RequestBody Map<String, Object> createData
    ) {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.createVolume(credentialInfo, createData);
    }

    @RequestMapping(value = "/servers/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    Header deleteServer(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String serverId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        return toastService.deleteServer(credentialInfo, serverId);
    }

    @RequestMapping(value = "/volumes/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    Header deleteVolume(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String volumeId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteVolume(credentialInfo, volumeId);
    }

    @RequestMapping(value = "/snapshots/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    Header deleteSnapshot(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String snapshotId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteSnapshot(credentialInfo, snapshotId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/servers", method = RequestMethod.POST)
    public @ResponseBody
    ServerInfo createServer(
            @RequestHeader(value = "credential") String credential,
            @RequestBody String createData,
            HttpSession session) throws Exception {

        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObj = null; // 중괄호에 들어갈 속성 정의 { "a" : "1", "b" : "2" }
        JSONObject jsonObj2 = new JSONObject();
        JSONObject jsonObj3 = new JSONObject();
        JSONObject jsonObj4 = new JSONObject();
        JSONObject jsonObj5 = new JSONObject();
        JSONObject jsonObj6 = new JSONObject();
        JSONArray jsonarray = new JSONArray(); // 대괄호 정의 [{ "a" : "1", "b" : "2" }]
        JSONArray jsonarray2 = new JSONArray();
        JSONArray jsonarray3 = new JSONArray();
        JSONObject finalJsonObject1 = new JSONObject(); // 중괄호로 감싸 대괄호의 이름을 정의함 { "c" : [{  "a" : "1", "b" : "2" }] }
        JSONObject finalJsonObject2 = new JSONObject();
        try {
            jsonObj = (JSONObject) jsonParser.parse(createData);
        } catch (ParseException e) {
        }

        jsonObj2.put("subnet", "4da4b581-4d69-4c2b-a6e0-51c25ea63966");
        jsonarray.add(jsonObj2);
        jsonObj3.put("name", jsonObj.get("security_groups"));
        jsonarray2.add(jsonObj3);
        jsonObj6.put("uuid", jsonObj.get("uuid"));
        jsonObj6.put("boot_index", "0");
        jsonObj6.put("device_name", "vda");
        jsonObj6.put("source_type", "image");
        jsonObj6.put("destination_type", "volume");
        jsonarray3.add(jsonObj6);
        jsonObj4.put("name", jsonObj.get("name"));
        jsonObj4.put("imageRef", jsonObj.get("imageRef"));
        jsonObj4.put("flavorRef", jsonObj.get("flavorRef"));
        jsonObj4.put("availability_zone", jsonObj.get("availability_zone"));
        jsonObj4.put("max_count", jsonObj.get("max_count"));
        jsonObj4.put("key_name", jsonObj.get("key_name"));
        jsonObj4.put("networks", jsonarray);
        jsonObj4.put("block_device_mapping_v2", jsonarray3);
        jsonObj4.put("security_groups", jsonarray2);
        jsonObj5.put("server", jsonObj4);

        return toastService.createServers(credentialInfo, jsonObj5);
    }

    @RequestMapping(value = "/servers/{id}/action", method = RequestMethod.POST)
    @ResponseBody
    public String serverAction(
            @RequestHeader(value = "credential") String credential,
            @RequestBody JSONObject data,
            @PathVariable(value = "id") String instanceId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.serverAction(credentialInfo, data, instanceId);
    }

     @RequestMapping(value = "/validate", method = RequestMethod.GET)
     @ResponseBody
     public Map<String, Boolean> checkValidate(
             @RequestHeader(value = "credential") String credential
     ) {

         CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

         return new HashMap<String, Boolean> (){{
             put("result", toastService.validateCredential(credentialInfo));
         }};
     }

    @RequestMapping(value = "/images", method = RequestMethod.GET)
    @ResponseBody
    public List<ImageDetailInfo> getPublicImages(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getImageDetails(credentialInfo);
    }


    @RequestMapping(value = "/images/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ImageDetailInfo getImagesDetails(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String imageId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));
        return toastService.getImagesDetails(credentialInfo, imageId);
    }

    @RequestMapping(value = "/images/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    Header deleteImage(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String imageId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteImage(credentialInfo, imageId);
    }

    @RequestMapping(value = "/zones", method = RequestMethod.GET)
    @ResponseBody
    public List<ZoneDetailInfo> getZone(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getZoneDetails(credentialInfo);
    }

    @RequestMapping(value = "/flavors", method = RequestMethod.GET)
    @ResponseBody
    public List<FlavorDetailInfo> getFlavor(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getFlavorDetails(credentialInfo);
    }

    @RequestMapping(value = "/keypairs", method = RequestMethod.POST)
    @ResponseBody
    public KeypairsDetailInfo postKeypair(
            @RequestHeader(value = "credential") String credential,
            @RequestBody String data
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.postKeypair(credentialInfo, data);
    }

    @RequestMapping(value = "/keypairs", method = RequestMethod.GET)
    @ResponseBody
    public List<Keypair> getKeypair(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getKeypairDetails(credentialInfo);
    }

    @RequestMapping(value = "/keypairs/{name}", method = RequestMethod.GET)
    @ResponseBody
    public KeypairsDetailInfo getKeypairsDetails(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "name") String keypairName
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getKeypairsDetails(credentialInfo, keypairName);
    }

    @RequestMapping(value = "/keypairs/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    Header deleteKeypair(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String keypairName
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteKeypair(credentialInfo, keypairName);
    }

    @RequestMapping(value = "/networks", method = RequestMethod.GET)
    @ResponseBody
    public List<Networks> getNetworks(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getNetworks(credentialInfo);
    }

    @RequestMapping(value = "/subnets", method = RequestMethod.GET)
    @ResponseBody
    public List<Subnets> getSubnets(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getSubnets(credentialInfo);
    }

    @RequestMapping(value = "/securitygroups", method = RequestMethod.GET)
    @ResponseBody
    public List<SecurityGroups> getSecurityGroups(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getSecurityGroups(credentialInfo);
    }

    @RequestMapping(value = "/securitygroups/{id}", method = RequestMethod.GET)
    @ResponseBody
    public SecurityGroupInfo getSecurityGroups(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String securityId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getSecurityGroups(credentialInfo, securityId);
    }

    @RequestMapping(value = "/securitygroups/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Header deleteSecurityGroup(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String securityId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteSecurityGroup(credentialInfo, securityId);
    }

    @RequestMapping(value = "/floatingIps", method = RequestMethod.GET)
    @ResponseBody
    public List<Floatingips> getFloatingips(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getFloatingips(credentialInfo);
    }

    @RequestMapping(value = "/floatingIps/{id}", method = RequestMethod.GET)
    @ResponseBody
    public FloatingipInfo getFloatingip(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String floatingIpId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getFloatingip(credentialInfo, floatingIpId);
    }

    @RequestMapping(value = "/floatingIps/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Header deleteFloatingip(
            @RequestHeader(value = "credential") String credential,
            @PathVariable(value = "id") String floatingIpId
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.deleteFloatingip(credentialInfo, floatingIpId);
    }

    @RequestMapping(value = "/tokens", method = RequestMethod.GET)
    @ResponseBody
    public TokensInfo getToken(
            @RequestHeader(value = "credential") String credential
    ) {
        CredentialInfo credentialInfo = ObjectSerializer.deserializedData(aes256Util.decrypt(credential));

        return toastService.getToken(credentialInfo);
    }
}