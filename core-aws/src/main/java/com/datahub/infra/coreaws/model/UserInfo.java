package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import software.amazon.awssdk.services.iam.model.User;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 5492928983108577267L;
    private String id;
    private String name;
    private String path;
    private String arn;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;

    public UserInfo() {

    }

    public UserInfo(User info) {
        this.id = info.userId();
        this.name = info.userName();
        this.path = info.path();
        this.arn = info.arn();
        this.createdAt = Timestamp.from(info.createDate());
    }
}
