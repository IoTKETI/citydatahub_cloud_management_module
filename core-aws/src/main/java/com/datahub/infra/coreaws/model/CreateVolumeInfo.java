package com.datahub.infra.coreaws.model;

import lombok.Data;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateVolumeInfo implements Serializable {
    private static final long serialVersionUID = -8908819334506024014L;
    private String availabilityZone;
    private Boolean encrypted;
    private int iops;
    private int size;
    private String snapshotId;
    private List<Tag> tags;
    private String volumeType;

    public CreateVolumeInfo(){

    }
}
