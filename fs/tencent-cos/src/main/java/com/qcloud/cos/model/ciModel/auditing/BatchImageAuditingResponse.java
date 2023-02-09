package com.qcloud.cos.model.ciModel.auditing;

import com.qcloud.cos.model.CiServiceResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片批量审核响应实体 参数详情参考：https://cloud.tencent.com/document/product/460/37318
 */
public class BatchImageAuditingResponse extends CiServiceResult {
    /**
     * 任务的详细信息
     */
    private List<BatchImageJobDetail> jobList;

    public List<BatchImageJobDetail> getJobList() {
        if (jobList == null) {
            jobList = new ArrayList<>();
        }
        return jobList;
    }

    public void setJobList(List<BatchImageJobDetail> jobsDetail) {
        this.jobList = jobsDetail;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("VideoAuditingResponse{");
        sb.append("jobsDetail=").append(jobList);
        sb.append('}');
        return sb.toString();
    }
}
