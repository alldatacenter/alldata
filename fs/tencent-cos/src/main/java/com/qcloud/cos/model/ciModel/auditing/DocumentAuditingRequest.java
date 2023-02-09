package com.qcloud.cos.model.ciModel.auditing;

import com.qcloud.cos.internal.CIServiceRequest;

/**
 * 文档审核 请求实体类
 */
public class DocumentAuditingRequest extends CIServiceRequest {
    private DocumentInputObject input;
    private Conf conf;
    private String jobId;

    public DocumentInputObject getInput() {
        if (input == null) {
            input = new DocumentInputObject();
        }
        return input;
    }

    public void setInput(DocumentInputObject input) {
        this.input = input;
    }

    public Conf getConf() {
        if (conf == null) {
            conf = new Conf();
        }
        return conf;
    }

    public void setConf(Conf conf) {
        this.conf = conf;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocumentAuditingRequest{");
        sb.append("input=").append(input);
        sb.append(", conf=").append(conf);
        sb.append(", jobId='").append(jobId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
