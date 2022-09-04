package com.alibaba.sreworks.health.domain;

import java.io.Serializable;
import java.util.Date;

public class AlertInstance implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Integer defId;

    private String appInstanceId;

    private String appComponentInstanceId;

    private String metricInstanceId;

    private String metricInstanceLabels;

    private Date gmtOccur;

    private String source;

    private String level;

    private String receivers;

    private String content;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Integer getDefId() {
        return defId;
    }

    public void setDefId(Integer defId) {
        this.defId = defId;
    }

    public String getAppInstanceId() {
        return appInstanceId;
    }

    public void setAppInstanceId(String appInstanceId) {
        this.appInstanceId = appInstanceId == null ? null : appInstanceId.trim();
    }

    public String getAppComponentInstanceId() {
        return appComponentInstanceId;
    }

    public void setAppComponentInstanceId(String appComponentInstanceId) {
        this.appComponentInstanceId = appComponentInstanceId == null ? null : appComponentInstanceId.trim();
    }

    public String getMetricInstanceId() {
        return metricInstanceId;
    }

    public void setMetricInstanceId(String metricInstanceId) {
        this.metricInstanceId = metricInstanceId == null ? null : metricInstanceId.trim();
    }

    public String getMetricInstanceLabels() {
        return metricInstanceLabels;
    }

    public void setMetricInstanceLabels(String metricInstanceLabels) {
        this.metricInstanceLabels = metricInstanceLabels == null ? null : metricInstanceLabels.trim();
    }

    public Date getGmtOccur() {
        return gmtOccur;
    }

    public void setGmtOccur(Date gmtOccur) {
        this.gmtOccur = gmtOccur;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? null : source.trim();
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level == null ? null : level.trim();
    }

    public String getReceivers() {
        return receivers;
    }

    public void setReceivers(String receivers) {
        this.receivers = receivers == null ? null : receivers.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        AlertInstance other = (AlertInstance) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getDefId() == null ? other.getDefId() == null : this.getDefId().equals(other.getDefId()))
            && (this.getAppInstanceId() == null ? other.getAppInstanceId() == null : this.getAppInstanceId().equals(other.getAppInstanceId()))
            && (this.getAppComponentInstanceId() == null ? other.getAppComponentInstanceId() == null : this.getAppComponentInstanceId().equals(other.getAppComponentInstanceId()))
            && (this.getMetricInstanceId() == null ? other.getMetricInstanceId() == null : this.getMetricInstanceId().equals(other.getMetricInstanceId()))
            && (this.getMetricInstanceLabels() == null ? other.getMetricInstanceLabels() == null : this.getMetricInstanceLabels().equals(other.getMetricInstanceLabels()))
            && (this.getGmtOccur() == null ? other.getGmtOccur() == null : this.getGmtOccur().equals(other.getGmtOccur()))
            && (this.getSource() == null ? other.getSource() == null : this.getSource().equals(other.getSource()))
            && (this.getLevel() == null ? other.getLevel() == null : this.getLevel().equals(other.getLevel()))
            && (this.getReceivers() == null ? other.getReceivers() == null : this.getReceivers().equals(other.getReceivers()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getDefId() == null) ? 0 : getDefId().hashCode());
        result = prime * result + ((getAppInstanceId() == null) ? 0 : getAppInstanceId().hashCode());
        result = prime * result + ((getAppComponentInstanceId() == null) ? 0 : getAppComponentInstanceId().hashCode());
        result = prime * result + ((getMetricInstanceId() == null) ? 0 : getMetricInstanceId().hashCode());
        result = prime * result + ((getMetricInstanceLabels() == null) ? 0 : getMetricInstanceLabels().hashCode());
        result = prime * result + ((getGmtOccur() == null) ? 0 : getGmtOccur().hashCode());
        result = prime * result + ((getSource() == null) ? 0 : getSource().hashCode());
        result = prime * result + ((getLevel() == null) ? 0 : getLevel().hashCode());
        result = prime * result + ((getReceivers() == null) ? 0 : getReceivers().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", defId=").append(defId);
        sb.append(", appInstanceId=").append(appInstanceId);
        sb.append(", appComponentInstanceId=").append(appComponentInstanceId);
        sb.append(", metricInstanceId=").append(metricInstanceId);
        sb.append(", metricInstanceLabels=").append(metricInstanceLabels);
        sb.append(", gmtOccur=").append(gmtOccur);
        sb.append(", source=").append(source);
        sb.append(", level=").append(level);
        sb.append(", receivers=").append(receivers);
        sb.append(", content=").append(content);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}