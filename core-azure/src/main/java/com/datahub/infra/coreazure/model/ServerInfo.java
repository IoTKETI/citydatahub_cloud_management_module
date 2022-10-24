package com.datahub.infra.coreazure.model;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ComputeResourceType;
import com.microsoft.azure.management.compute.ComputeSku;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ServerInfo implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(ServerInfo.class);

    private static final long serialVersionUID = -2097698631127792498L;
    private String id;
    private String name;
    private String location;
    private String powerState;
    private String provisioningState;
    private String resourceGroupName;
    private String osType;
    private String primaryPublicIPAddress;
    private String primaryPrivateIP;
    private String size;
    private Integer cpu;
    private Double memory;
    private Integer disk;
    private Map<String,String> tags;
    private String type;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String createId;

    public ServerInfo (){

    }

    public ServerInfo(VirtualMachine vm){
        this.id = vm.id();
        this.name = vm.inner().name();
        this.location = vm.inner().location();
        this.type = vm.type().split("/")[1];
        if(vm.powerState()!=null) this.powerState = vm.powerState().toString().split("/")[1];
        this.provisioningState = vm.provisioningState();
        this.resourceGroupName = vm.resourceGroupName();
        this.osType = vm.osType().toString();
        if(vm.getPrimaryPublicIPAddress() != null) this.primaryPublicIPAddress=vm.getPrimaryPublicIPAddress().ipAddress();
        this.primaryPrivateIP=vm.getPrimaryNetworkInterface().primaryPrivateIP();
        this.size = vm.size().toString();
        this.tags = vm.tags().size() != 0 ? vm.tags() : null;
    }

    public ServerInfo(Azure azure, VirtualMachine vm){

        String region = "koreacentral";
        List<SizeInfo> vmSizeList = new ArrayList<>();

        Region regionInfo = Region.fromName(region);
        PagedList<ComputeSku> skuList = azure.computeSkus().listbyRegionAndResourceType(regionInfo, ComputeResourceType.VIRTUALMACHINES);
        for(ComputeSku sku : skuList) {
            if (sku.resourceType().equals(ComputeResourceType.VIRTUALMACHINES)) {
                SizeInfo vmSizeInfo = new SizeInfo(sku);
                String equ = vmSizeInfo.getOffering() + "_" + vmSizeInfo.getVmSize();
                logger.error("test Equ = {}", equ);
                if(vm.size().toString().equals(equ)){
                    logger.error("vm.size().toString().equals(equ) = {}", vm.size().toString().equals(equ));
                    this.cpu = vmSizeInfo.getVCPUs();
                    this.memory = vmSizeInfo.getMemoryGB();
                    this.disk = vmSizeInfo.getMaxDataDiskCount();
//                    vmSizeList.add(vmSizeInfo);
                }
            }
        }
        logger.error("vmSizeList is = {}", vmSizeList);

        String _id = vm.id();
        byte[] _idBytes = _id.getBytes();
        this.id = java.util.Base64.getEncoder().encodeToString(_idBytes);
        this.name = vm.inner().name();
        this.location = vm.inner().location();
        this.type = vm.type().split("/")[1];
        if(vm.powerState()!=null) this.powerState = vm.powerState().toString().split("/")[1];
        this.provisioningState = vm.provisioningState();
        this.resourceGroupName = vm.resourceGroupName();
        this.osType = vm.osType().toString();
        if(vm.getPrimaryPublicIPAddress() != null) this.primaryPublicIPAddress=vm.getPrimaryPublicIPAddress().ipAddress();
        this.primaryPrivateIP=vm.getPrimaryNetworkInterface().primaryPrivateIP();
        this.tags = vm.tags().size() != 0 ? vm.tags() : null;
    }
}
