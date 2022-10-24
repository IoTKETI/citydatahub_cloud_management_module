package com.datahub.infra.coreaws.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class KeyPairInfo implements Serializable {

    private static final long serialVersionUID = 6819890572779908722L;
    private String name;
    private String fingerprint;

    public KeyPairInfo() {

    }

    public KeyPairInfo(software.amazon.awssdk.services.ec2.model.KeyPairInfo info) {
        this.name = info.keyName();
        this.fingerprint = info.keyFingerprint();
    }
}
