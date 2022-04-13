package com.datahub.infra.coretoast.model;

import com.google.api.client.util.Key;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author kkm
 * @date 2019.3.22
 * @brief TOAST 서버 생성용 모델
 */
@Data
public class ImageDetailInfo implements Serializable {

    private static final long serialVersionUID = -8629918608557087127L;

    @Key private String status;
    @Key private String name;
    @Key private String[] tags;
    @Key private String visibility;
    @Key private String id;
    @Key private String checksum;
    @Key private String file;
    @Key private String owner;
    @Key private BigInteger size;
    @Key private String schema;
    @Key private String self;
    @Key private String container_format;
    @Key private String created_at;
    @Key private String disk_format;
    @Key private String updated_at;
    @Key private Integer min_disk;
//    @Key private Boolean protected;
    @Key private Integer min_ram;
    @Key private String deprecate_date;
    @Key private String monitoring_agent;
    @Key private String release_date;
    @Key private String os_version;
    @Key private String os_type;
    @Key private String os_locale;
    @Key private String login_username;
    @Key private String description;
    @Key private String hypervisor_type;
    @Key private String os_distro;
    @Key private String project_domain;
    @Key private String tc_env;
    @Key private String os_architecture;


    public ImageDetailInfo() {
        this.description = getDescription();
    }
}
