package com.qcloud.cos.model.ciModel.job;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理结果实体类
 */
public class DocProcessResult {

    /**
     * 文档产物信息列表
     */
    private List<DocProcessPageInfo> docProcessPageInfoList;

    /**
     * 预览任务产物的总数
     */
    private String totalPageCount;

    /**
     * 预览任务产物的成功数
     */
    private String succPageCount;

    /**
     * 预览任务产物的失败数
     */
    private String failPageCount;

    /**
     * 预览产物目标格式
     */
    private String tgtType;

    /**
     * 预览任务的 Sheet 总数（源文件为 Excel 特有参数）
     */
    private String totalSheetCount;


    public List<DocProcessPageInfo> getDocProcessPageInfoList() {
        if (docProcessPageInfoList == null) {
            docProcessPageInfoList = new ArrayList<>();
        }
        return docProcessPageInfoList;
    }

    public void setDocProcessPageInfoList(List<DocProcessPageInfo> docProcessPageInfoList) {
        this.docProcessPageInfoList = docProcessPageInfoList;
    }

    public String getTotalPageCount() {
        return totalPageCount;
    }

    public void setTotalPageCount(String totalPageCount) {
        this.totalPageCount = totalPageCount;
    }

    public String getSuccPageCount() {
        return succPageCount;
    }

    public void setSuccPageCount(String succPageCount) {
        this.succPageCount = succPageCount;
    }

    public String getFailPageCount() {
        return failPageCount;
    }

    public void setFailPageCount(String failPageCount) {
        this.failPageCount = failPageCount;
    }

    public String getTgtType() {
        return tgtType;
    }

    public void setTgtType(String tgtType) {
        this.tgtType = tgtType;
    }

    public String getTotalSheetCount() {
        return totalSheetCount;
    }

    public void setTotalSheetCount(String totalSheetCount) {
        this.totalSheetCount = totalSheetCount;
    }

    @Override
    public String toString() {
        return "DocProcessResult{" +
                "docProcessPageInfoList=" + docProcessPageInfoList +
                ", totalPageCount='" + totalPageCount + '\'' +
                ", succPageCount='" + succPageCount + '\'' +
                ", failPageCount='" + failPageCount + '\'' +
                ", tgtType='" + tgtType + '\'' +
                ", totalSheetCount='" + totalSheetCount + '\'' +
                '}';
    }
}
