package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Snapshot;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class SnapshotInfo implements Serializable {

    private static final long serialVersionUID = 2963592155116037461L;
    private String id;
    private String name;
    private int size;
    private String description;
    private String state;
    private Boolean encrypted;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    private String volumeId;
    private String progress;
    private String ownerId;

    public SnapshotInfo() {

    }

    public SnapshotInfo(Snapshot info) {
        this.id = info.snapshotId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        else this.name = "";
        this.state = info.state().toString();
        this.size = info.volumeSize();
        this.description = info.description();
        this.encrypted = info.encrypted();
        this.volumeId = info.volumeId();
        this.progress = info.progress();
        this.ownerId = info.ownerId();
        this.createdAt = Timestamp.from(info.startTime());
    }
}
