package com.qcloud.cos.model.ciModel.auditing;

public class OcrResults {
    private String text;
    private String keywords;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OcrResults{");
        sb.append("text='").append(text).append('\'');
        sb.append(", keywords='").append(keywords).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
