package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.compute.ImageDataDisk;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Data
public class ImageInfo implements Serializable {
    private static final long serialVersionUID = -4784891911634751281L;
    private String id;
    private String name;
    private String blobUri;
    private String caching;
    private int diskSizeGB;
    private String osState;
    private String storageAccountType;
    private String location;
    private String resourceGroupName;
    private Boolean isCreatedFromVirtualMachine;
    private String sourceVirtualMachineId;
    private String dnsName;
    private String osType;
    private String provisioningState;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private Map<String,String> tags;
    private List<ImageDataDiskInfo> imageDataDiskInfo = new ArrayList();

    public ImageInfo() {

    }

    public ImageInfo(VirtualMachineCustomImage vmCustomImage){
        this.id = vmCustomImage.id();
        this.name= vmCustomImage.name();
        this.blobUri= vmCustomImage.osDiskImage().blobUri();
        this.caching= vmCustomImage.osDiskImage().caching().name();
        this.diskSizeGB= vmCustomImage.osDiskImage().diskSizeGB();
        this.osState= vmCustomImage.osDiskImage().osState().name();
        this.storageAccountType=vmCustomImage.osDiskImage().storageAccountType().toString();
        this.location= vmCustomImage.regionName();
        this.resourceGroupName=vmCustomImage.resourceGroupName();
        this.isCreatedFromVirtualMachine= vmCustomImage.isCreatedFromVirtualMachine();
        this.sourceVirtualMachineId= vmCustomImage.sourceVirtualMachineId();
        this.dnsName= vmCustomImage.sourceVirtualMachineId();
        this.osType = vmCustomImage.osDiskImage().osType().toString();
        this.tags = vmCustomImage.tags().size() != 0 ? vmCustomImage.tags() : null;
        this.provisioningState = vmCustomImage.inner().provisioningState();

        Map<Integer,ImageDataDisk> imageDataDiskMap = vmCustomImage.dataDiskImages();
        Iterator<Integer> keys = imageDataDiskMap.keySet().iterator();
        while( keys.hasNext() ){
            int key = keys.next();
            this.imageDataDiskInfo.add(new ImageDataDiskInfo(imageDataDiskMap.get(key)));
        }
    }
}
