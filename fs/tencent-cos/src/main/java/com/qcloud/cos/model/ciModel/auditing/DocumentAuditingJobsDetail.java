package com.qcloud.cos.model.ciModel.auditing;

import java.util.ArrayList;
import java.util.List;

public class DocumentAuditingJobsDetail {
    /**
     * 新创建任务的 ID
     */
    private String jobId;
    /**
     * 任务的状态，为 Submitted、Snapshoting、Success、Failed、Auditing 其中一个
     */
    private String state;
    /**
     * 任务的创建时间
     */
    private String creationTime;

    /**
     * 错误码，只有 State 为 Failed 时有意义
     */
    private String code;
    /**
     * 错误描述，只有 State 为 Failed 时有意义
     */
    private String message;

    /**
     * 文档地址
     */
    private String url;

    /**
     * 文档的cos相对地址
     */
    private String object;

    /**
     * 表示本次判定的审核结果
     * 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）
     */
    private String suggestion;

    /**
     * 转换的图片总数量
     */
    private String pageCount;

    /**
     * 请求标识
     */
    private String dataId;

    /**
     * 文档转换为图片后，具体每张图片的审核结果信息，只返回带有违规结果的图片
     */
    private List<DocumentResultInfo> pageSegment;

    /**
     * 该字段用于返回命中的审核场景及对应的结果
     */
    private DocumentResultInfo labels;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }

    public List<DocumentResultInfo> getPageSegment() {
        if (pageSegment == null) {
            pageSegment = new ArrayList<>();
        }
        return pageSegment;
    }

    public void setPageSegment(List<DocumentResultInfo> pageSegment) {
        if (pageSegment == null) {
            pageSegment = new ArrayList<>();
        }
        this.pageSegment = pageSegment;
    }

    public DocumentResultInfo getLabels() {
        if (labels == null) {
            labels = new DocumentResultInfo();
        }
        return labels;
    }

    public void setLabels(DocumentResultInfo labels) {
        this.labels = labels;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocumentAuditingJobsDetail{");
        sb.append("jobId='").append(jobId).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", creationTime='").append(creationTime).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", object='").append(object).append('\'');
        sb.append(", suggestion='").append(suggestion).append('\'');
        sb.append(", pageCount='").append(pageCount).append('\'');
        sb.append(", dataId='").append(dataId).append('\'');
        sb.append(", pageSegment=").append(pageSegment);
        sb.append(", labels=").append(labels);
        sb.append('}');
        return sb.toString();
    }
}
