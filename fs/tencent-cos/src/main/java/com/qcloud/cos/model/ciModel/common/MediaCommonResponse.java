package com.qcloud.cos.model.ciModel.common;


import com.qcloud.cos.model.CiServiceResult;

/**
 * 媒体处理 公用返回实体
 */
public class MediaCommonResponse extends CiServiceResult {
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
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 修改时间
     */
    private String updateTime;

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

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaCommonResponse{");
        sb.append("requestId='").append(requestId).append('\'');
        sb.append(", totalCount='").append(totalCount).append('\'');
        sb.append(", pageNumber='").append(pageNumber).append('\'');
        sb.append(", pageSize='").append(pageSize).append('\'');
        sb.append(", createTime='").append(createTime).append('\'');
        sb.append(", updateTime='").append(updateTime).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
