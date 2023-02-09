package com.qcloud.cos.model.ciModel.auditing;


import com.qcloud.cos.internal.CIServiceRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片批量审核请求实体 参数详情参考：https://cloud.tencent.com/document/product/436/56289
 */
public class BatchImageAuditingRequest extends CIServiceRequest {
    /**
     * 审核类型，拥有 porn（涉黄识别）、terrorist（涉暴恐识别）、politics（涉政识别）、ads（广告识别）四种。用户可选择多种识别类型，
     * 例如 detectType=porn,ads 表示对图片进行涉黄及广告审核
     */
    private Conf conf;

    private List<BatchImageAuditingInputObject> inputList;

    private String jobId;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<BatchImageAuditingInputObject> getInputList() {
        if (inputList == null) {
            inputList = new ArrayList<>();
        }
        return inputList;
    }

    public void setInputList(List<BatchImageAuditingInputObject> inputList) {
        this.inputList = inputList;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BatchImageAuditingRequest{");
        sb.append("conf=").append(conf);
        sb.append(", inputList=").append(inputList);
        sb.append(", jobId='").append(jobId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
