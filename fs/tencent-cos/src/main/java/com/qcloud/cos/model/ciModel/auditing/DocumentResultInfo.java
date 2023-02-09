package com.qcloud.cos.model.ciModel.auditing;

/**
 * 截图信息，只返回违规的截图信息
 */
public class DocumentResultInfo extends SnapshotInfo {
    private String text;
    private String pageNumber;
    private String sheetNumber;
    private String label;
    private String suggestion;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getSheetNumber() {
        return sheetNumber;
    }

    public void setSheetNumber(String sheetNumber) {
        this.sheetNumber = sheetNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocumentResultInfo{");
        sb.append("text='").append(text).append('\'');
        sb.append(", pageNumber='").append(pageNumber).append('\'');
        sb.append(", sheetNumber='").append(sheetNumber).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
