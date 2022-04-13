package com.datahub.infra.coreopenstack.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.datahub.infra.coreopenstack.util.JsonDateDeserializer;
import com.datahub.infra.coreopenstack.util.JsonDateSerializer;
import lombok.Data;
import org.openstack4j.model.storage.block.Volume;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class VolumeInfo2 implements Serializable {

    private static final long serialVersionUID = -7056474800842404026L;
    private String id;
    private String name;
    private String description;
    private String volumeType;
    @JsonProperty("volumeState")
    private String state;
    @JsonProperty("volumeSize")
    private int size;
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonDeserialize(using = JsonDateDeserializer.class)
    private Timestamp createdAt;
    public VolumeInfo2() {

    }

    public VolumeInfo2(Volume info) {
        if(info != null) {
            this.id = info.getId();
            this.name = info.getName();
            if (this.name == null || this.name.equals("")) {
                this.name = this.id;
            }

            this.volumeType = info.getVolumeType();

            this.description = info.getDescription();
            this.state = checkState(info.getStatus().value());
            this.size = info.getSize();
            this.createdAt = new Timestamp(info.getCreated().getTime());
            List<VolumeAttachmentInfo> volumeAttachmentInfos = new ArrayList<>();

            for (int i = 0; i < info.getAttachments().size(); i++) {
                VolumeAttachmentInfo volumeAttachmentInfo = new VolumeAttachmentInfo(info.getAttachments().get(i));
                volumeAttachmentInfos.add(volumeAttachmentInfo);
            }
        }
    }

    public String checkState(String state){
        switch (state){
            case "creating" :
                return "pending";

            case "unrecognized":
                return "notUsing";

            case "deleting":
                return "deallocating";

            default:
                return state;
        }
    }

}
