package com.datahub.infra.coreopenstack.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreopenstack.util.JsonDateDeserializer;
import com.datahub.infra.coreopenstack.util.JsonDateSerializer;
import lombok.Data;
import org.openstack4j.model.compute.Server;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
public class ServerInfo implements Serializable {

    private static final long serialVersionUID = 2897465774550361154L;
    private String id;
    private String host;
    private String name;
    private String state;
    private String imageId;
    private String imageName;
    private String flavorId;
    private String flavorName;
    private int cpu;
    private int memory;
    private int disk;
    private String powerState;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    private String projectId;
    private String projectName;
    private List<String> volumes;
    private Map<String, String> metaData;
    private String keyName;
    private String taskState;

    public ServerInfo() {
    }

    public ServerInfo(Server server) {
        if(server == null) return;

        this.id = server.getId();
        this.host = server.getHost();
        this.name = server.getName();
        this.state = server.getStatus().value();
        this.imageId = server.getImageId();
        if(server.getImage() != null) this.imageName = server.getImage().getName();
        this.flavorId = server.getFlavorId();
        if(server.getFlavor() != null) {
            this.flavorName = server.getFlavor().getName();
            this.cpu = server.getFlavor().getVcpus();
            this.memory = server.getFlavor().getRam();
            this.disk = server.getFlavor().getDisk();
        }
        this.metaData = server.getMetadata();

        Iterator<String> keys = server.getAddresses().getAddresses().keySet().iterator();
        this.projectId = server.getTenantId();

        this.powerState = server.getPowerState();
        this.createdAt = new Timestamp(server.getCreated().getTime());
        this.volumes = server.getOsExtendedVolumesAttached();
        this.keyName = server.getKeyName();
        this.taskState = server.getTaskState();
    }
}
