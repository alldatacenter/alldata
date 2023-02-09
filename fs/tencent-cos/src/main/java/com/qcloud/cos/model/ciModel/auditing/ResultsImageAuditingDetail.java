package com.qcloud.cos.model.ciModel.auditing;

public class ResultsImageAuditingDetail extends BaseAuditingDetail {
    private String url;
    private String text;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ResultsImageAuditingDetail{");
        sb.append("url='").append(url).append('\'');
        sb.append(", text='").append(text).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
