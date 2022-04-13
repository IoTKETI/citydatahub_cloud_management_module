package com.datahub.infra.coredb.service;

import com.datahub.infra.core.model.ImageDetailInfo;
import com.datahub.infra.core.model.ImageInfo;

import java.util.List;
import java.util.Map;

public interface ImageService {
    public List<ImageInfo> getImages(Map<String, Object> params);
    public List<ImageDetailInfo> getImageDetails(String type, String location);
    public ImageDetailInfo getImageDetail(String id);
    public int getImageDetailIdCount(String id);
    public int createImageDetail(ImageDetailInfo info);
    public int updateImageDetail(ImageDetailInfo info);
    public int deleteImageDetail(ImageDetailInfo info);
}
