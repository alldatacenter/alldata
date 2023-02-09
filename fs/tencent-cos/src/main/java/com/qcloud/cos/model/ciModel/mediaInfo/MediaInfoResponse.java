package com.qcloud.cos.model.ciModel.mediaInfo;


import com.qcloud.cos.model.CiServiceResult;

/**
 * MediaInfo 媒体信息返回包装类 详情见：https://cloud.tencent.com/document/product/460/38935
 */
public class MediaInfoResponse extends CiServiceResult {
    /**
     * 媒体信息实体对象
     */
    private MediaInfoObjcet mediaInfo;

    public MediaInfoObjcet getMediaInfo() {
        if (mediaInfo==null){
            mediaInfo = new MediaInfoObjcet();
        }
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfoObjcet mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    @Override
    public String toString() {
        return "MediaInfoResponse{" +
                "mediaInfo=" + mediaInfo +
                '}';
    }
}
