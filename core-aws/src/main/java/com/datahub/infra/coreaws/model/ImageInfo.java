package com.datahub.infra.coreaws.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreaws.util.JsonDateDeserializer;
import com.datahub.infra.coreaws.util.JsonDateSerializer;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.model.Image;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Data
public class ImageInfo implements Serializable {

    private static final long serialVersionUID = 2848036757775386584L;
    private static Logger logger = LoggerFactory.getLogger(ImageInfo.class);

    private String id;
    private String name;
    private String imageName;
    private String imageType;
    private String source;
    private String osType;
    private String architecture;
    private String state;
    private String hypervisor;
    private String virtualizationType;
    private String ownerId;
    private String rootDeviceName;
    private String rootDeviceType;
    private String enaSupport;
    private String description;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public ImageInfo() {

    }

    public ImageInfo(Image info) {
        this.id = info.imageId();
        if(info.tags().size() > 0) this.name = info.tags().get(0).value();
        else this.name = "";
        this.imageName = info.name();
        this.state = info.stateAsString();
        this.imageType = info.imageTypeAsString();
        this.source = info.imageLocation();
        this.osType = getOsType(info.platformAsString());
        this.architecture = info.architectureAsString();
        this.hypervisor = info.hypervisorAsString();
        this.virtualizationType = info.virtualizationTypeAsString();
        this.ownerId = info.ownerId();
        this.rootDeviceName = info.rootDeviceName();
        this.rootDeviceType = info.rootDeviceTypeAsString();
        this.rootDeviceType = info.rootDeviceTypeAsString();
        try {
            this.createdAt =  new Timestamp(dateFormatter.parse(info.creationDate()).getTime());
        } catch (ParseException e) {
            logger.error("Failed to initialize ImageInfo : '{}'", e.getMessage());
            e.printStackTrace();
        }
    }

    private String getOsType(String osType) {
        String osTypeName = "";

        if(osType == null) {
            String imageName = this.name.toUpperCase();
            if(imageName.contains("RHEL")) {
                osTypeName = "RHEL";
            } else if(imageName.contains("SUSE")) {
                osTypeName = "SUSE";
            } else {
                osTypeName = "Linux";
            }
        } else {
            if(osType.toUpperCase().equals("WINDOWS")) osTypeName = "Windows";
        }

        return osTypeName;
    }
}
