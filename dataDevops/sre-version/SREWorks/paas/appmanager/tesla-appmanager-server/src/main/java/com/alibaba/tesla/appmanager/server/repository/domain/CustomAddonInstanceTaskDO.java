package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
public class CustomAddonInstanceTaskDO implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String namespaceId;

    private String addonId;

    private String addonName;

    private String addonVersion;

    private String addonAttrs;

    private String taskStatus;

    private Long taskProcessId;

    private Long deployAppId;

    private String taskErrorMessage;

    private String taskExt;

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

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId == null ? null : namespaceId.trim();
    }

    public String getAddonId() {
        return addonId;
    }

    public void setAddonId(String addonId) {
        this.addonId = addonId == null ? null : addonId.trim();
    }

    public String getAddonName() {
        return addonName;
    }

    public void setAddonName(String addonName) {
        this.addonName = addonName == null ? null : addonName.trim();
    }

    public String getAddonVersion() {
        return addonVersion;
    }

    public void setAddonVersion(String addonVersion) {
        this.addonVersion = addonVersion == null ? null : addonVersion.trim();
    }

    public String getAddonAttrs() {
        return addonAttrs;
    }

    public void setAddonAttrs(String addonAttrs) {
        this.addonAttrs = addonAttrs == null ? null : addonAttrs.trim();
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus == null ? null : taskStatus.trim();
    }

    public Long getTaskProcessId() {
        return taskProcessId;
    }

    public void setTaskProcessId(Long taskProcessId) {
        this.taskProcessId = taskProcessId;
    }

    public Long getDeployAppId() {
        return deployAppId;
    }

    public void setDeployAppId(Long deployAppId) {
        this.deployAppId = deployAppId;
    }

    public String getTaskErrorMessage() {
        return taskErrorMessage;
    }

    public void setTaskErrorMessage(String taskErrorMessage) {
        this.taskErrorMessage = taskErrorMessage == null ? null : taskErrorMessage.trim();
    }

    public String getTaskExt() {
        return taskExt;
    }

    public void setTaskExt(String taskExt) {
        this.taskExt = taskExt == null ? null : taskExt.trim();
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
        CustomAddonInstanceTaskDO other = (CustomAddonInstanceTaskDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
                && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
                && (this.getNamespaceId() == null ? other.getNamespaceId() == null : this.getNamespaceId().equals(other.getNamespaceId()))
                && (this.getAddonId() == null ? other.getAddonId() == null : this.getAddonId().equals(other.getAddonId()))
                && (this.getAddonName() == null ? other.getAddonName() == null : this.getAddonName().equals(other.getAddonName()))
                && (this.getAddonVersion() == null ? other.getAddonVersion() == null : this.getAddonVersion().equals(other.getAddonVersion()))
                && (this.getAddonAttrs() == null ? other.getAddonAttrs() == null : this.getAddonAttrs().equals(other.getAddonAttrs()))
                && (this.getTaskStatus() == null ? other.getTaskStatus() == null : this.getTaskStatus().equals(other.getTaskStatus()))
                && (this.getTaskProcessId() == null ? other.getTaskProcessId() == null : this.getTaskProcessId().equals(other.getTaskProcessId()))
                && (this.getDeployAppId() == null ? other.getDeployAppId() == null : this.getDeployAppId().equals(other.getDeployAppId()))
                && (this.getTaskErrorMessage() == null ? other.getTaskErrorMessage() == null : this.getTaskErrorMessage().equals(other.getTaskErrorMessage()))
                && (this.getTaskExt() == null ? other.getTaskExt() == null : this.getTaskExt().equals(other.getTaskExt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getNamespaceId() == null) ? 0 : getNamespaceId().hashCode());
        result = prime * result + ((getAddonId() == null) ? 0 : getAddonId().hashCode());
        result = prime * result + ((getAddonName() == null) ? 0 : getAddonName().hashCode());
        result = prime * result + ((getAddonVersion() == null) ? 0 : getAddonVersion().hashCode());
        result = prime * result + ((getAddonAttrs() == null) ? 0 : getAddonAttrs().hashCode());
        result = prime * result + ((getTaskStatus() == null) ? 0 : getTaskStatus().hashCode());
        result = prime * result + ((getTaskProcessId() == null) ? 0 : getTaskProcessId().hashCode());
        result = prime * result + ((getDeployAppId() == null) ? 0 : getDeployAppId().hashCode());
        result = prime * result + ((getTaskErrorMessage() == null) ? 0 : getTaskErrorMessage().hashCode());
        result = prime * result + ((getTaskExt() == null) ? 0 : getTaskExt().hashCode());
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
        sb.append(", namespaceId=").append(namespaceId);
        sb.append(", addonId=").append(addonId);
        sb.append(", addonName=").append(addonName);
        sb.append(", addonVersion=").append(addonVersion);
        sb.append(", addonAttrs=").append(addonAttrs);
        sb.append(", taskStatus=").append(taskStatus);
        sb.append(", taskProcessId=").append(taskProcessId);
        sb.append(", deployAppId=").append(deployAppId);
        sb.append(", taskErrorMessage=").append(taskErrorMessage);
        sb.append(", taskExt=").append(taskExt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}