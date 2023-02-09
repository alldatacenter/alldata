package com.qcloud.cos.model.ciModel.auditing;

public class BatchImageAuditingInputObject extends AuditingInputObject {
    private String interval;
    private String maxFrames;

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getMaxFrames() {
        return maxFrames;
    }

    public void setMaxFrames(String maxFrames) {
        this.maxFrames = maxFrames;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BatchImageAuditingInputObject{");
        sb.append("interval='").append(interval).append('\'');
        sb.append(", maxFrames='").append(maxFrames).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
