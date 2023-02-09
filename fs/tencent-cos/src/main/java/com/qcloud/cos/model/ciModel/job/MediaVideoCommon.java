package com.qcloud.cos.model.ciModel.job;

import java.io.Serializable;
/**
 * 媒体处理 任务video公共实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaVideoCommon implements Serializable {

    /**
     * 编解码格式 仅限 gif，webp
     */
    private String codec;
    /**
     * 宽
     */
    private String width;
    /**
     * 高
     */
    private String height;
    /**
     * 帧率
     */
    private String fps;

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    @Override
    public String toString() {
        return "MediaVideoCommon{" +
                "codec='" + codec + '\'' +
                ", width='" + width + '\'' +
                ", height='" + height + '\'' +
                ", fps='" + fps + '\'' +
                '}';
    }
}
