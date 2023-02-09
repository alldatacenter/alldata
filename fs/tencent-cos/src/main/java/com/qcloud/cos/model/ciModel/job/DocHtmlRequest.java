package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.internal.CosServiceRequest;

/**
 * 文档预览任务发起请求类
 */
public class DocHtmlRequest extends CosServiceRequest {

    /**
     * cos桶名称
     */
    private String bucketName;

    /**
     * 对象文件名，例如 folder/document.pdf
     */
    private String objectKey;

    /**
     * 源数据的后缀类型，当前文档转换根据 COS 对象的后缀名来确定源数据类型。当 COS 对象没有后缀名时，可以设置该值
     */
    private String srcType;

    /**
     * 需转换的文档页码，默认从1开始计数；表格文件中 page 表示转换的第 X 个 sheet 的第 X 张图
     */
    private String page;

    /**
     * 转换后的图片处理参数，支持 <基础图片处理> 所有处理参数，多个处理参数可通过 <管道操作符> 分隔，从而实现在一次访问中按顺序对图片进行不同处理
     * 基础图片处理 https://cloud.tencent.com/document/product/460/6924
     * 管道操作符 https://cloud.tencent.com/document/product/460/15293
     */
    private String ImageParams;

    /**
     * 表格文件参数，转换第 X 个表，默认为1
     */
    private String sheet;

    /**
     * 转换输出目标文件类型：
     * png，转成 png 格式的图片文件
     * jpg，转成 jpg 格式的图片文件
     * pdf，转成pdf 格式文件
     * 如果传入的格式未能识别，默认使用 jpg 格式
     */
    private String dstType;

    /**
     * 文档的打开密码，如果需要转换有密码的文档，请设置该字段
     */
    private String password;

    /**
     * 是否隐藏批注和应用修订，默认为 0。
     * 0：隐藏批注，应用修订
     * 1：显示批注和修订
     */
    private String comment;

    /**
     * 表格文件转换纸张方向，0代表垂直方向，非0代表水平方向，默认为0
     */
    private String excelPaperDirection;

    /**
     * 生成预览图的图片质量，取值范围为 [1, 100]，默认值100。 例如取值为100，代表生成图片质量为100%
     */
    private String quality;
    /**
     * 预览图片的缩放参数，取值范围为 [10, 200]， 默认值100。 例如取值为200，代表图片缩放比例为200% 即放大两倍
     */
    private String scale;


    private DocType type = DocType.html;


    public enum DocType {
        html, jpg, png
    }


    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getSrcType() {
        return srcType;
    }

    public void setSrcType(String srcType) {
        this.srcType = srcType;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getImageParams() {
        return ImageParams;
    }

    public void setImageParams(String imageParams) {
        ImageParams = imageParams;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public String getDstType() {
        return dstType;
    }

    public void setDstType(String dstType) {
        this.dstType = dstType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getExcelPaperDirection() {
        return excelPaperDirection;
    }

    public void setExcelPaperDirection(String excelPaperDirection) {
        this.excelPaperDirection = excelPaperDirection;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public DocType getType() {
        return type;
    }

    public void setType(DocType type) {
        this.type = type;
    }


    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocHtmlRequest{");
        sb.append("bucketName='").append(bucketName).append('\'');
        sb.append(", objectKey='").append(objectKey).append('\'');
        sb.append(", srcType='").append(srcType).append('\'');
        sb.append(", page='").append(page).append('\'');
        sb.append(", ImageParams='").append(ImageParams).append('\'');
        sb.append(", sheet='").append(sheet).append('\'');
        sb.append(", dstType='").append(dstType).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", comment='").append(comment).append('\'');
        sb.append(", excelPaperDirection='").append(excelPaperDirection).append('\'');
        sb.append(", quality='").append(quality).append('\'');
        sb.append(", scale='").append(scale).append('\'');
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
