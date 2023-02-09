package com.qcloud.cos.model.ciModel.auditing;


import java.util.ArrayList;
import java.util.List;

public class WebpageAuditingJobsDetail {
    private List<ResultsImageAuditingDetail> imageResults = new ArrayList<>();
    private List<ResultsTextAuditingDetail> textResults = new ArrayList<>();
    private String jobId;
    private String label;
    private String pageCount;
    private String state;
    private String suggestion;
    private String url;
    private String creationTime;

    public List<ResultsImageAuditingDetail> getImageResults() {
        return imageResults;
    }

    public void setImageResults(List<ResultsImageAuditingDetail> imageResults) {
        this.imageResults = imageResults;
    }

    public List<ResultsTextAuditingDetail> getTextResults() {
        return textResults;
    }

    public void setTextResults(List<ResultsTextAuditingDetail> textResults) {
        this.textResults = textResults;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("WebpageAuditingJobsDetail{");
        sb.append("imageResults=").append(imageResults);
        sb.append(", textResults=").append(textResults);
        sb.append(", jobId='").append(jobId).append('\'');
        sb.append(", label='").append(label).append('\'');
        sb.append(", pageCount='").append(pageCount).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", suggestion='").append(suggestion).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", creationTime='").append(creationTime).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
