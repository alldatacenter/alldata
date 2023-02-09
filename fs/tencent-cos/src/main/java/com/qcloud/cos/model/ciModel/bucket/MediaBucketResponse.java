package com.qcloud.cos.model.ciModel.bucket;

import com.qcloud.cos.model.ciModel.common.MediaCommonResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据万象 媒体bucket查询接口响应实体 详情见 https://cloud.tencent.com/document/product/460/38914
 */
public class MediaBucketResponse extends MediaCommonResponse {
    private List<MediaBucketObject> MediaBucketList;

    public MediaBucketResponse() {
        MediaBucketList = new ArrayList<>();
    }

    public List<MediaBucketObject> getMediaBucketList() {
        return MediaBucketList;
    }

    public void setMediaBucketList(List<MediaBucketObject> mediaBucketList) {
        MediaBucketList = mediaBucketList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaBucketResponse{");
        sb.append("MediaBucketList=").append(MediaBucketList);
        sb.append('}');
        return sb.toString();
    }
}
