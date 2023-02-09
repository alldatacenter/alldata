package com.qcloud.cos.model.ciModel.job;

/**
 * 文档处理参数实体类
 */
public class DocProcessObject {
    /**
     * 源数据的后缀类型
     * 默认根据input cos对象后缀名来确定源数据类型
     * 当cos对象没有后缀名时,可以设置该值
     */
    private String srcType;
    /**
     * 转换输出目标文件类型：
     * jpg，转成 jpg 格式的图片文件
     * png，转成 png 格式的图片文件
     * pdf，转成 pdf 格式文件
     * 默认使用 jpg 格式
     */
    private String tgtType;
    /**
     * 表格文件参数，转换第 X 个表，默认为1；
     * 设置 SheetId 为0，即转换文档中全部表 (仅在源文件为表格文件时生效)
     */
    private String sheetId;
    /**
     * 从第 X 页开始转换；
     * 在表格文件中，一张表可能分割为多页转换，生成多张图片。StartPage 表示从指定 SheetId 的第 X 页开始转换。默认为1
     */
    private String startPage;
    /**
     * 转换至第 X 页；
     * 在表格文件中，一张表可能分割为多页转换，生成多张图片。EndPage 表示转换至指定 SheetId 的第 X 页。默认为-1，即转换全部页
     */
    private String endPage;
    /**
     * 转换后的图片处理参数
     * 支持基础图片处理所有处理参数，多个处理参数可通过管道操作符分隔
     * 实现在一次访问中按顺序对图片进行不同处理
     * 基础图片处理详情参见：https://cloud.tencent.com/document/product/460/6924
     * 管道分隔符参见：https://cloud.tencent.com/document/product/460/15293
     */
    private String imageParams;
    /**
     * 文档的打开密码，如果需要转换有密码的文档，请设置该字段
     */
    private String docPassword;
    /**
     * 是否隐藏批注和应用修订，默认为 0。
     * 0：隐藏批注，应用修订
     * 1：显示批注和修订
     */
    private String comments;
    /**
     * 表格文件转换纸张方向，0代表垂直方向，非0代表水平方向，默认为0
     */
    private String paperDirection;
    /**
     * 生成预览图的图片质量，取值范围 [1-100]，默认值100。 例：值为100，代表生成图片质量为100%
     */
    private String quality;
    /**
     * 预览图片的缩放参数，取值范围[10-200]， 默认值100。 例：值为200，代表图片缩放比例为200% 即放大两倍
     */
    private String zoom;

    public String getSrcType() {
        return srcType;
    }

    public void setSrcType(String srcType) {
        this.srcType = srcType;
    }

    public String getTgtType() {
        return tgtType;
    }

    public void setTgtType(String tgtType) {
        this.tgtType = tgtType;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getStartPage() {
        return startPage;
    }

    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    public String getEndPage() {
        return endPage;
    }

    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    public String getImageParams() {
        return imageParams;
    }

    public void setImageParams(String imageParams) {
        this.imageParams = imageParams;
    }

    public String getDocPassword() {
        return docPassword;
    }

    public void setDocPassword(String docPassword) {
        this.docPassword = docPassword;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPaperDirection() {
        return paperDirection;
    }

    public void setPaperDirection(String paperDirection) {
        this.paperDirection = paperDirection;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getZoom() {
        return zoom;
    }

    public void setZoom(String zoom) {
        this.zoom = zoom;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocProcessObject{");
        sb.append("srcType='").append(srcType).append('\'');
        sb.append(", tgtType='").append(tgtType).append('\'');
        sb.append(", sheetId='").append(sheetId).append('\'');
        sb.append(", startPage='").append(startPage).append('\'');
        sb.append(", endPage='").append(endPage).append('\'');
        sb.append(", imageParams='").append(imageParams).append('\'');
        sb.append(", docPassword='").append(docPassword).append('\'');
        sb.append(", comments='").append(comments).append('\'');
        sb.append(", paperDirection='").append(paperDirection).append('\'');
        sb.append(", quality='").append(quality).append('\'');
        sb.append(", zoom='").append(zoom).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
