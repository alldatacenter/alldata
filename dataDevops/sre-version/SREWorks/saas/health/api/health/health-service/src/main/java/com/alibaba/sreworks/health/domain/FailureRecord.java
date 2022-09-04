package com.alibaba.sreworks.health.domain;

import java.io.Serializable;
import java.util.Date;

public class FailureRecord implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private Long failureId;

    private Integer defId;

    private String appInstanceId;

    private String appComponentInstanceId;

    private Long incidentId;

    private String name;

    private String level;

    private Date gmtOccur;

    private Date gmtRecovery;

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

    public Long getFailureId() {
        return failureId;
    }

    public void setFailureId(Long failureId) {
        this.failureId = failureId;
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

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level == null ? null : level.trim();
    }

    public Date getGmtOccur() {
        return gmtOccur;
    }

    public void setGmtOccur(Date gmtOccur) {
        this.gmtOccur = gmtOccur;
    }

    public Date getGmtRecovery() {
        return gmtRecovery;
    }

    public void setGmtRecovery(Date gmtRecovery) {
        this.gmtRecovery = gmtRecovery;
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
        FailureRecord other = (FailureRecord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getFailureId() == null ? other.getFailureId() == null : this.getFailureId().equals(other.getFailureId()))
            && (this.getDefId() == null ? other.getDefId() == null : this.getDefId().equals(other.getDefId()))
            && (this.getAppInstanceId() == null ? other.getAppInstanceId() == null : this.getAppInstanceId().equals(other.getAppInstanceId()))
            && (this.getAppComponentInstanceId() == null ? other.getAppComponentInstanceId() == null : this.getAppComponentInstanceId().equals(other.getAppComponentInstanceId()))
            && (this.getIncidentId() == null ? other.getIncidentId() == null : this.getIncidentId().equals(other.getIncidentId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getLevel() == null ? other.getLevel() == null : this.getLevel().equals(other.getLevel()))
            && (this.getGmtOccur() == null ? other.getGmtOccur() == null : this.getGmtOccur().equals(other.getGmtOccur()))
            && (this.getGmtRecovery() == null ? other.getGmtRecovery() == null : this.getGmtRecovery().equals(other.getGmtRecovery()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getFailureId() == null) ? 0 : getFailureId().hashCode());
        result = prime * result + ((getDefId() == null) ? 0 : getDefId().hashCode());
        result = prime * result + ((getAppInstanceId() == null) ? 0 : getAppInstanceId().hashCode());
        result = prime * result + ((getAppComponentInstanceId() == null) ? 0 : getAppComponentInstanceId().hashCode());
        result = prime * result + ((getIncidentId() == null) ? 0 : getIncidentId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getLevel() == null) ? 0 : getLevel().hashCode());
        result = prime * result + ((getGmtOccur() == null) ? 0 : getGmtOccur().hashCode());
        result = prime * result + ((getGmtRecovery() == null) ? 0 : getGmtRecovery().hashCode());
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
        sb.append(", failureId=").append(failureId);
        sb.append(", defId=").append(defId);
        sb.append(", appInstanceId=").append(appInstanceId);
        sb.append(", appComponentInstanceId=").append(appComponentInstanceId);
        sb.append(", incidentId=").append(incidentId);
        sb.append(", name=").append(name);
        sb.append(", level=").append(level);
        sb.append(", gmtOccur=").append(gmtOccur);
        sb.append(", gmtRecovery=").append(gmtRecovery);
        sb.append(", content=").append(content);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}