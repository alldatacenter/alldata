package com.qcloud.cos.model.ciModel.job;

/**
 * 文档预览产物生成信息
 */
public class DocProcessPageInfo {
    /**
     * 预览产物生成的 cos 桶路径
     */
    private String tgtUri;
    /**
     * 预览产物页码,源文件为 Excel 格式时表示 SheetId
     */
    private String pageNo;
    /**
     * 当前预览产物在整个源文件中的序号（源文件为 Excel 特有参数）
     */
    private String picIndex;
    /**
     * 当前预览产物在 Sheet 中的序号（源文件为 Excel 特有参数）
     */
    private String picNum;

    /**
     * 	当前 Sheet 生成的图片总数（源文件为 Excel 特有参数）
     */
    private String xSheetPics;

    public String getTgtUri() {
        return tgtUri;
    }

    public void setTgtUri(String tgtUri) {
        this.tgtUri = tgtUri;
    }

    public String getPageNo() {
        return pageNo;
    }

    public void setPageNo(String pageNo) {
        this.pageNo = pageNo;
    }

    public String getPicIndex() {
        return picIndex;
    }

    public void setPicIndex(String picIndex) {
        this.picIndex = picIndex;
    }

    public String getPicNum() {
        return picNum;
    }

    public void setPicNum(String picNum) {
        this.picNum = picNum;
    }

    public String getxSheetPics() {
        return xSheetPics;
    }

    public void setxSheetPics(String xSheetPics) {
        this.xSheetPics = xSheetPics;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocProcessPageInfo{");
        sb.append("tgtUri='").append(tgtUri).append('\'');
        sb.append(", pageNo='").append(pageNo).append('\'');
        sb.append(", picIndex='").append(picIndex).append('\'');
        sb.append(", picNum='").append(picNum).append('\'');
        sb.append(", xSheetPics='").append(xSheetPics).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
