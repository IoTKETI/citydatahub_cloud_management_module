package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.iam.model.Group;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 5469695807266076959L;
    private String id;
    private String name;
    private String path;
    private String arn;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;

    public GroupInfo() {

    }

    public GroupInfo(Group info) {
        this.id = info.groupId();
        this.name = info.groupName();
        this.path = info.path();
        this.arn = info.arn();
        this.createdAt = Timestamp.from(info.createDate());
    }
}
