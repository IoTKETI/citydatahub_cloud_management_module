package com.datahub.infra.coreopenstack.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreopenstack.util.JsonDateDeserializer;
import com.datahub.infra.coreopenstack.util.JsonDateSerializer;
import lombok.Data;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.SecurityGroup;
import org.openstack4j.model.compute.Server;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
public class ServerInfo2 implements Serializable {

    private static final long serialVersionUID = 2897465774550361154L;
    private String id;
    private String name;
    @JsonProperty("sourceType")
    private String imageName;
    private String imageId;
    private String flavorName;
    private String zone;

    @JsonProperty("serverState")
    private String state;

    @JsonProperty("keyPair")
    private String keyName;
    private int cpu;
    private int memory;
    private int disk;
    @JsonProperty("networkId")
    private String network;
    private String ip;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    @JsonProperty("securityGroupName")
    private String securityGroup;
    private Boolean volumeCreated;
    private String test1;
    private String test2;
    private List<AddressInfo> addresses;

    public ServerInfo2() {
    }

    public ServerInfo2(Server server, List<NetworkInfo> networkInfoList) {
        if (server == null) return;

        String tempNetName;

        this.id = server.getId();
        this.name = server.getName();

        this.state = checkState(server.getStatus().value());

        this.imageId = server.getImageId();

        this.zone = server.getAvailabilityZone();

        this.keyName = server.getKeyName();


        if (server.getFlavor() != null) {
            this.flavorName = server.getFlavor().getName();
            this.cpu = server.getFlavor().getVcpus();
            this.memory = server.getFlavor().getRam();
            this.disk = server.getFlavor().getDisk();
        }

        Iterator<String> keys = server.getAddresses().getAddresses().keySet().iterator();
        List<AddressInfo> list = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();

            List<? extends Address> addresses = server.getAddresses().getAddresses().get(key);

            for (int i = 0; i < addresses.size(); i++) {
                AddressInfo info = new AddressInfo(addresses.get(i));
                info.setNetworkName(key);
                list.add(info);

                if (info.getType().equals("floating")) {
                    this.ip = info.getAddr();
                }
                tempNetName = info.getNetworkName();
                this.network=setNetworkID(networkInfoList,tempNetName);
            }
        }
        this.addresses = list;
        this.createdAt = new Timestamp(server.getCreated().getTime());
        List<? extends SecurityGroup> securityGroups =server.getSecurityGroups();
        if(securityGroups!=null){
            for(int i=0;i<securityGroups.size();i++) {
                this.securityGroup = securityGroups.get(i).getName();
            }
        }
        else{
            this.securityGroup=null;
        }

        if (server.getHost() == null) {
            this.volumeCreated = false;
        }else{
            this.volumeCreated = true;
        }
    }

    public String setNetworkID(List<NetworkInfo> networkInfoList, String networkName) {
        String networkId=null;
        for (int i = 0; i < networkInfoList.size(); i++) {
            if (networkInfoList.get(i).getId().equals(networkName) ){
                networkId=networkInfoList.get(i).getId();
            }
        }
        return networkId;
    }

    public String checkState(String state){
        switch (state){
            case "build" :
                return "pending";

            case "active":
                return "running";

            case "shutoff":
                return "stopped";

            default:
                return state;
        }
    }

    public AddressInfo getAddressInfo(String addr) {
        for(int i=0; i<this.addresses.size(); i++) {
            if(addresses.get(i).getAddr().equals(addr)) {
                return addresses.get(i);
            }
        }

        return null;
    }
}
