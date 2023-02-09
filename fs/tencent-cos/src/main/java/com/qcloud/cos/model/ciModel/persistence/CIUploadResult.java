package com.qcloud.cos.model.ciModel.persistence;

public class CIUploadResult {
    private OriginalInfo originalInfo;
    private ProcessResults processResults;
    public OriginalInfo getOriginalInfo() {
        return originalInfo;
    }

    public void setOriginalInfo(OriginalInfo originalInfo) {
        this.originalInfo = originalInfo;
    }

    public ProcessResults getProcessResults() {
        return processResults;
    }

    public void setProcessResults(ProcessResults processResults) {
        this.processResults = processResults;
    }
}
