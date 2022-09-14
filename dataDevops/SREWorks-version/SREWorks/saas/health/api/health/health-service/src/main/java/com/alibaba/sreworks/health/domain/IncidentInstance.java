package com.alibaba.sreworks.health.domain;

import java.io.Serializable;
import java.util.Date;

public class IncidentInstance implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Integer defId;

    private String appInstanceId;

    private String appComponentInstanceId;

    private Date gmtOccur;

    private Date gmtLastOccur;

    private Integer occurTimes;

    private Date gmtRecovery;

    private String source;

    private String traceId;

    private String spanId;

    private Date gmtSelfHealingStart;

    private Date gmtSelfHealingEnd;

    private String selfHealingStatus;

    private String options;

    private String description;

    private String cause;

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

    public Date getGmtOccur() {
        return gmtOccur;
    }

    public void setGmtOccur(Date gmtOccur) {
        this.gmtOccur = gmtOccur;
    }

    public Date getGmtLastOccur() {
        return gmtLastOccur;
    }

    public void setGmtLastOccur(Date gmtLastOccur) {
        this.gmtLastOccur = gmtLastOccur;
    }

    public Integer getOccurTimes() {
        return occurTimes;
    }

    public void setOccurTimes(Integer occurTimes) {
        this.occurTimes = occurTimes;
    }

    public Date getGmtRecovery() {
        return gmtRecovery;
    }

    public void setGmtRecovery(Date gmtRecovery) {
        this.gmtRecovery = gmtRecovery;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? null : source.trim();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId == null ? null : traceId.trim();
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId == null ? null : spanId.trim();
    }

    public Date getGmtSelfHealingStart() {
        return gmtSelfHealingStart;
    }

    public void setGmtSelfHealingStart(Date gmtSelfHealingStart) {
        this.gmtSelfHealingStart = gmtSelfHealingStart;
    }

    public Date getGmtSelfHealingEnd() {
        return gmtSelfHealingEnd;
    }

    public void setGmtSelfHealingEnd(Date gmtSelfHealingEnd) {
        this.gmtSelfHealingEnd = gmtSelfHealingEnd;
    }

    public String getSelfHealingStatus() {
        return selfHealingStatus;
    }

    public void setSelfHealingStatus(String selfHealingStatus) {
        this.selfHealingStatus = selfHealingStatus == null ? null : selfHealingStatus.trim();
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options == null ? null : options.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause == null ? null : cause.trim();
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
        IncidentInstance other = (IncidentInstance) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getDefId() == null ? other.getDefId() == null : this.getDefId().equals(other.getDefId()))
            && (this.getAppInstanceId() == null ? other.getAppInstanceId() == null : this.getAppInstanceId().equals(other.getAppInstanceId()))
            && (this.getAppComponentInstanceId() == null ? other.getAppComponentInstanceId() == null : this.getAppComponentInstanceId().equals(other.getAppComponentInstanceId()))
            && (this.getGmtOccur() == null ? other.getGmtOccur() == null : this.getGmtOccur().equals(other.getGmtOccur()))
            && (this.getGmtLastOccur() == null ? other.getGmtLastOccur() == null : this.getGmtLastOccur().equals(other.getGmtLastOccur()))
            && (this.getOccurTimes() == null ? other.getOccurTimes() == null : this.getOccurTimes().equals(other.getOccurTimes()))
            && (this.getGmtRecovery() == null ? other.getGmtRecovery() == null : this.getGmtRecovery().equals(other.getGmtRecovery()))
            && (this.getSource() == null ? other.getSource() == null : this.getSource().equals(other.getSource()))
            && (this.getTraceId() == null ? other.getTraceId() == null : this.getTraceId().equals(other.getTraceId()))
            && (this.getSpanId() == null ? other.getSpanId() == null : this.getSpanId().equals(other.getSpanId()))
            && (this.getGmtSelfHealingStart() == null ? other.getGmtSelfHealingStart() == null : this.getGmtSelfHealingStart().equals(other.getGmtSelfHealingStart()))
            && (this.getGmtSelfHealingEnd() == null ? other.getGmtSelfHealingEnd() == null : this.getGmtSelfHealingEnd().equals(other.getGmtSelfHealingEnd()))
            && (this.getSelfHealingStatus() == null ? other.getSelfHealingStatus() == null : this.getSelfHealingStatus().equals(other.getSelfHealingStatus()))
            && (this.getOptions() == null ? other.getOptions() == null : this.getOptions().equals(other.getOptions()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getCause() == null ? other.getCause() == null : this.getCause().equals(other.getCause()));
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
        result = prime * result + ((getGmtOccur() == null) ? 0 : getGmtOccur().hashCode());
        result = prime * result + ((getGmtLastOccur() == null) ? 0 : getGmtLastOccur().hashCode());
        result = prime * result + ((getOccurTimes() == null) ? 0 : getOccurTimes().hashCode());
        result = prime * result + ((getGmtRecovery() == null) ? 0 : getGmtRecovery().hashCode());
        result = prime * result + ((getSource() == null) ? 0 : getSource().hashCode());
        result = prime * result + ((getTraceId() == null) ? 0 : getTraceId().hashCode());
        result = prime * result + ((getSpanId() == null) ? 0 : getSpanId().hashCode());
        result = prime * result + ((getGmtSelfHealingStart() == null) ? 0 : getGmtSelfHealingStart().hashCode());
        result = prime * result + ((getGmtSelfHealingEnd() == null) ? 0 : getGmtSelfHealingEnd().hashCode());
        result = prime * result + ((getSelfHealingStatus() == null) ? 0 : getSelfHealingStatus().hashCode());
        result = prime * result + ((getOptions() == null) ? 0 : getOptions().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getCause() == null) ? 0 : getCause().hashCode());
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
        sb.append(", gmtOccur=").append(gmtOccur);
        sb.append(", gmtLastOccur=").append(gmtLastOccur);
        sb.append(", occurTimes=").append(occurTimes);
        sb.append(", gmtRecovery=").append(gmtRecovery);
        sb.append(", source=").append(source);
        sb.append(", traceId=").append(traceId);
        sb.append(", spanId=").append(spanId);
        sb.append(", gmtSelfHealingStart=").append(gmtSelfHealingStart);
        sb.append(", gmtSelfHealingEnd=").append(gmtSelfHealingEnd);
        sb.append(", selfHealingStatus=").append(selfHealingStatus);
        sb.append(", options=").append(options);
        sb.append(", description=").append(description);
        sb.append(", cause=").append(cause);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}