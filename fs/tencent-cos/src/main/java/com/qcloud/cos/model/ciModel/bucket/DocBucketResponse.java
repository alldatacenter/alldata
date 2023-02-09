package com.qcloud.cos.model.ciModel.bucket;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据万象 文档预览 bucket查询接口响应实体 详情见 https://cloud.tencent.com/document/product/460/46945
 */
public class DocBucketResponse {
    private List<DocBucketObject> docBucketObjectList;

    /**
     * 请求id
     */
    private String requestId;
    /**
     * 查询的总条数 用于list相关接口
     */
    private String totalCount;
    /**
     * 页码
     */
    private String pageNumber;
    /**
     * 每页展示数量
     */
    private String pageSize;


    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(String totalCount) {
        this.totalCount = totalCount;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<DocBucketObject> getDocBucketObjectList() {
        if (docBucketObjectList == null) {
            docBucketObjectList = new ArrayList<>();
        }
        return docBucketObjectList;
    }

    public void setDocBucketObjectList(List<DocBucketObject> docBucketObjectList) {
        this.docBucketObjectList = docBucketObjectList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocBucketResponse{");
        sb.append("docBucketObjectList=").append(docBucketObjectList);
        sb.append(", requestId='").append(requestId).append('\'');
        sb.append(", totalCount='").append(totalCount).append('\'');
        sb.append(", pageNumber='").append(pageNumber).append('\'');
        sb.append(", pageSize='").append(pageSize).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
