package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.compute.ImageDataDisk;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ImageDataDiskInfo implements Serializable {
    private static final long serialVersionUID = -6410958659176365547L;
    private int lun;
    private String blobUri;
    private String caching;
    private String storageAccountType;
    private Map<String,String> tags;

    public ImageDataDiskInfo() {

    }

    public ImageDataDiskInfo(ImageDataDisk imageDataDisk){
        this.lun = imageDataDisk.lun();
        this.blobUri= imageDataDisk.blobUri();
        this.caching= imageDataDisk.caching().name();
        this.storageAccountType=imageDataDisk.storageAccountType().toString();
    }
}
