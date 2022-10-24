package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.storage.StorageAccount;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class StorageAccountInfo implements Serializable {
    private static final long serialVersionUID = 949733368653378851L;
    private String id;
    private String name;
    private String type;
    private String kind;
    private String resourceGroupName;
    private String location;
    private String subscriptionId;
    private String subscriptionDisplayName;
    private String primaryStatus;
    private String secondaryStatus;
    private String accessTier;
    private Map<String,String> tags;

    public StorageAccountInfo() {

    }

    public StorageAccountInfo(StorageAccount storageAccount){
        this.primaryStatus = "Primary : "+storageAccount.inner().statusOfPrimary();
        this.secondaryStatus = storageAccount.inner().statusOfSecondary()!=null?"Secondary : "+storageAccount.inner().statusOfSecondary():"";
        this.id = storageAccount.id();
        this.name = storageAccount.name();
        this.type = storageAccount.type();
        this.kind = storageAccount.inner().kind().toString();
        this.resourceGroupName = storageAccount.resourceGroupName();
        this.location = storageAccount.inner().location();
        this.accessTier = storageAccount.accessTier()!=null?storageAccount.accessTier().toString():"";
        this.tags = storageAccount.tags().size() != 0 ? storageAccount.tags() : null;
    }
}
