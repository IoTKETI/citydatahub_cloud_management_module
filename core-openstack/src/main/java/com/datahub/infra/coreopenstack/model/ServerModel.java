package com.datahub.infra.coreopenstack.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServerModel implements Serializable {

    private static final long serialVersionUID = 2897465774550361154L;
    private String access_ip_v4;
    private String access_ip_v6;
    private Boolean all_tenants;
    private String auto_disk_config;
    private String config_drive;
    private String created_at;
    private Boolean deleted;
    private String description;
    private String flavor;
    private String host;
    private String hostname;
    private String image;
    private String ip;
    private String ip6;
    private String kernel_id;
    private String key_name;
    private Integer launch_index;
    private String launched_at;
    private Integer limit;
    private String locked_by;
    private String marker;
    private String name;
    private String node;
    private Integer power_state;
    private Integer progress;
    private String project_id;
    private String ramdisk_id;
    private String reservation_id;
    private String root_device_name;
    private Boolean soft_deleted;
    private String sort_dir;
    private String sort_key;
    private String status;
    private String task_state;
    private String terminated_at;
    private String user_id;
    private String uuid;
    private String vm_state;
    private String tags;
    private Boolean locked;
    private ServerModel servermodel_list[];

}
