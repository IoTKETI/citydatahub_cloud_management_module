package com.datahub.infra.coreazure.model;

import com.microsoft.azure.management.resources.Subscription;
import lombok.Data;

import java.io.Serializable;


@Data
public class SubscriptionInfo implements Serializable {
    private static final long serialVersionUID = 4781951722306104293L;
    private String subscriptionId;
    private String displayName;
    private String state;
    public SubscriptionInfo(){

    }
    public SubscriptionInfo(Subscription subscription) {
        this.subscriptionId = subscription.inner().subscriptionId();
        this.displayName = subscription.inner().displayName();
        this.state = subscription.inner().state().name();
    }
}
