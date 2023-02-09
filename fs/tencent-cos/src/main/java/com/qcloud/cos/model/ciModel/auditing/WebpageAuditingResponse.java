package com.qcloud.cos.model.ciModel.auditing;

/**
 * 音频审核响应实体 参数详情参考：https://cloud.tencent.com/document/product/460/53396
 */
public class WebpageAuditingResponse {
    /**
     * 任务的详细信息
     */
    private WebpageAuditingJobsDetail jobsDetail = new WebpageAuditingJobsDetail();
    private String requestId;

    public WebpageAuditingJobsDetail getJobsDetail() {
        return jobsDetail;
    }

    public void setJobsDetail(WebpageAuditingJobsDetail jobsDetail) {
        this.jobsDetail = jobsDetail;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("WebpageAuditingResponse{");
        sb.append("jobsDetail=").append(jobsDetail);
        sb.append(", requestId='").append(requestId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
