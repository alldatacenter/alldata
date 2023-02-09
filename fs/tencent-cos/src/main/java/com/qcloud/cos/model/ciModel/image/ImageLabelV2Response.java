package com.qcloud.cos.model.ciModel.image;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取图片标签接口响应实体 https://cloud.tencent.com/document/product/460/39082
 */
public class ImageLabelV2Response {
    /**
     * Web网络版标签结果数组。如未选择web场景，则为空。注意：此字段可能不存在，表示取不到有效值
     */
    private List<LobelV2> webLabels;

    /**
     * Camera摄像头版标签结果数组。如未选择camera场景，则为空。注意：此字段可能不存在，表示取不到有效值
     */
    private List<LobelV2> cameraLabels;

    /**
     * Album相册版标签结果数组。如未选择album场景，则为空。注意：此字段可能不存在，表示取不到有效值
     */
    private List<LobelV2> albumLabels;

    /**
     * News新闻版标签结果数组。如未选择news场景，则为空。新闻版目前为测试阶段，暂不提供每个标签的一级、二级分类信息的输出。
     * 注意：此字段可能不存在，表示取不到有效值
     */
    private List<LobelV2> newsLabels;

    /**
     * 非实拍标签注意：此字段可能不存在，表示取不到有效值
     */
    private List<LobelV2> noneCamLabels;

    /**
     * 识别结果 注意：此字段可能不存在，表示取不到有效值
     */
    private List<LocationLabel> productLabels;

    public List<LobelV2> getWebLabels() {
        if (webLabels == null) {
            webLabels = new ArrayList<>();
        }
        return webLabels;
    }

    public void setWebLabels(List<LobelV2> webLabels) {
        this.webLabels = webLabels;
    }

    public List<LobelV2> getCameraLabels() {
        if (cameraLabels == null) {
            cameraLabels = new ArrayList<>();
        }
        return cameraLabels;
    }

    public void setCameraLabels(List<LobelV2> cameraLabels) {
        this.cameraLabels = cameraLabels;
    }

    public List<LobelV2> getAlbumLabels() {
        if (albumLabels == null) {
            albumLabels = new ArrayList<>();
        }
        return albumLabels;
    }

    public void setAlbumLabels(List<LobelV2> albumLabels) {
        this.albumLabels = albumLabels;
    }

    public List<LobelV2> getNewsLabels() {
        if (newsLabels == null) {
            newsLabels = new ArrayList<>();
        }
        return newsLabels;
    }

    public void setNewsLabels(List<LobelV2> newsLabels) {
        this.newsLabels = newsLabels;
    }

    public List<LobelV2> getNoneCamLabels() {
        if (noneCamLabels == null) {
            noneCamLabels = new ArrayList<>();
        }
        return noneCamLabels;
    }

    public void setNoneCamLabels(List<LobelV2> noneCamLabels) {
        this.noneCamLabels = noneCamLabels;
    }

    public List<LocationLabel> getProductLabels() {
        if (productLabels == null) {
            productLabels = new ArrayList<>();
        }
        return productLabels;
    }

    public void setProductLabels(List<LocationLabel> productLabels) {
        this.productLabels = productLabels;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageLabelV2Response{");
        sb.append("webLabels=").append(webLabels);
        sb.append(", cameraLabels=").append(cameraLabels);
        sb.append(", albumLabels=").append(albumLabels);
        sb.append(", newsLabels=").append(newsLabels);
        sb.append(", noneCamLabels=").append(noneCamLabels);
        sb.append(", locationLabels=").append(productLabels);
        sb.append('}');
        return sb.toString();
    }
}
