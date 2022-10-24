package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;

@Data
public class MetadataServers implements Serializable {
    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String os_distro;
    @Key private String description;
    @Key private String os_version;
    @Key private String project_domain;
    @Key private String hypervisor_type;
    @Key private String monitoring_agent;
    @Key private String image_name;
    @Key private String volume_size;
    @Key private String os_architecture;
    @Key private String login_username;
    @Key private String os_type;
    @Key private String tc_env;

//    @Override
//    public String toString() {
//        return "Metadata{" +
//                "key=" + key +
//                '}';
//    }


}
