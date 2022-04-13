package com.datahub.infra.coreopenstack.model;

import lombok.Data;
import org.openstack4j.model.compute.Image;

import java.io.Serializable;
import java.util.Date;

@Data
public class NovaImageInfo implements Serializable {
    private static final long serialVersionUID = 5925883795531924279L;
    private String id;
    private String name;
    private Image.Status status;
    private int minDisk;
    private int minRam;
    private long size;
    private int progress;
    private boolean isSnapshot;
    private Date created;
    private Date updated;

    public NovaImageInfo() {}

    public NovaImageInfo(Image image) {
        this.id = image.getId();
        this.name = image.getName();
        this.created = image.getCreated();
        this.updated = image.getUpdated();
        this.size = image.getSize();
        this.minDisk = image.getMinDisk();
        this.minRam = image.getMinRam();
        this.progress = image.getProgress();
        this.status = image.getStatus();
        this.isSnapshot = image.isSnapshot();
    }
}
