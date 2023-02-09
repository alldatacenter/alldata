package com.qcloud.cos.model.ciModel.image;

/**
 * 标签信息
 */
public class Lobel {
    /**
     * 算法对于Name的置信度，0-100之间，值越高，表示对于Name越确定
     */
    private String confidence;

    /**
     * 图片标签名
     */
    private String name;

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Lobel{");
        sb.append("confidence='").append(confidence).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
