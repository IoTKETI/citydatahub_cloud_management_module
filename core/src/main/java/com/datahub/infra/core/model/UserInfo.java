package com.datahub.infra.core.model;

import com.datahub.infra.core.util.JsonDateDeserializer;
import com.datahub.infra.core.util.JsonDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 4705455919358933691L;
    private String id;
    private String newId;
    private String groupId;
    private String groupName;
    private String name;
    private String password;
    private Boolean enabled;
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Timestamp createdAt;
    private String email;
    private String contract;
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Timestamp login;
    private int loginCount;
    private String description;
    private Boolean admin;
    private int roleCount;
    private String roleId;

}
