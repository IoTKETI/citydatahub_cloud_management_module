package com.datahub.infra.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.core.Constants;
import com.datahub.infra.core.util.JsonDateDeserializer;
import com.datahub.infra.core.util.JsonDateSerializer;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ActionInfo {
    private String id;
    private String userName;
    private String groupId;
    private String content;
    private Constants.ACTION_RESULT result;
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Timestamp createdAt;
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Timestamp updatedAt;
    private String resultDetail;
    private String userId;
    private String targetId;
    private String targetName;
    private Constants.ACTION_CODE actionCode;
    private Constants.HISTORY_TYPE type;
    private Object object;
}
