package com.datahub.infra.coretoast.model;

import com.datahub.infra.coretoast.util.JsonDateDeserializer;
import com.datahub.infra.coretoast.util.JsonDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author consine2c
 * @date 2020.5.25
 * @brief TOAST Volume Model
 */
@Data
public class Servers implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String status;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @Key private String updated;
    @Key private String hostId;
    @Key private Addresses addresses;
    @Key private List<Links> links;
    @Key private String key_name;
    @Key private Image image;
    @Key private Flavor flavor;
    @Key private String id;
    @Key private List<SecurityGroupsServer> security_groups;
    @Key private String name;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    @Key private String created;
    @Key private String tenant_id;
    @Key private String accessIPv4;
    @Key private String accessIPv6;
    @Key private String config_drive;
    @Key private MetadataServers metadata;
}
