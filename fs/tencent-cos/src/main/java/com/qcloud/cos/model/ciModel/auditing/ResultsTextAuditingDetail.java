package com.qcloud.cos.model.ciModel.auditing;


public class ResultsTextAuditingDetail extends BaseAuditingDetail {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ResultsTextAuditingDetail{");
        sb.append('}');
        return sb.toString();
    }
}
