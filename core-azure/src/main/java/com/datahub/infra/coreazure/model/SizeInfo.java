package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.compute.ComputeSku;
import com.microsoft.azure.management.compute.ResourceSkuCapabilities;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SizeInfo implements Serializable {
    private static final long serialVersionUID = -4239231582326875852L;
    private String offering;
    private String vmSize;
    private String family;
    private Integer vCPUs;
    private Double memoryGB;
    private Integer maxDataDiskCount;
    private String premiumIO;

    public SizeInfo() {

    }

    public SizeInfo(ComputeSku sku) {
        this.offering = sku.inner().tier();
        this.vmSize= sku.inner().size();
        this.family = sku.inner().family();

        List<ResourceSkuCapabilities> list = sku.inner().capabilities();
        for(ResourceSkuCapabilities capability : list) {
            switch (capability.name()) {
                case "vCPUs":
                    this.vCPUs = Integer.valueOf(capability.value());
                    break;
                case "MemoryGB":
                    this.memoryGB = Double.valueOf(capability.value());
                    break;
                case "MaxDataDiskCount":
                    this.maxDataDiskCount = Integer.valueOf(capability.value());
                    break;
                case "PremiumIO":
                    this.premiumIO = Boolean.valueOf(capability.value()) ? "Yes" : "No";
                    break;
            }
        }
    }
}