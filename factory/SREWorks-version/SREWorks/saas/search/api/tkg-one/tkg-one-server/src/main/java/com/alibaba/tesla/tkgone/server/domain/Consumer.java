package com.alibaba.tesla.tkgone.server.domain;

import java.io.Serializable;
import java.util.Date;

public class Consumer implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String modifier;

    private String creator;

    private String importConfig;

    private String sourceInfo;

    private String sourceType;

    private String client;

    private String offset;

    private String status;

    private String name;

    private String enable;

    private String appName;

    private String userImportConfig;

    private Integer effectiveThreshold;

    private String notifiers;

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

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier == null ? null : modifier.trim();
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getImportConfig() {
        return importConfig;
    }

    public void setImportConfig(String importConfig) {
        this.importConfig = importConfig == null ? null : importConfig.trim();
    }

    public String getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(String sourceInfo) {
        this.sourceInfo = sourceInfo == null ? null : sourceInfo.trim();
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType == null ? null : sourceType.trim();
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client == null ? null : client.trim();
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset == null ? null : offset.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable == null ? null : enable.trim();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    public String getUserImportConfig() {
        return userImportConfig;
    }

    public void setUserImportConfig(String userImportConfig) {
        this.userImportConfig = userImportConfig == null ? null : userImportConfig.trim();
    }

    public Integer getEffectiveThreshold() {
        return effectiveThreshold;
    }

    public void setEffectiveThreshold(Integer effectiveThreshold) {
        this.effectiveThreshold = effectiveThreshold;
    }

    public String getNotifiers() {
        return notifiers;
    }

    public void setNotifiers(String notifiers) {
        this.notifiers = notifiers == null ? null : notifiers.trim();
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
        Consumer other = (Consumer) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getModifier() == null ? other.getModifier() == null : this.getModifier().equals(other.getModifier()))
            && (this.getCreator() == null ? other.getCreator() == null : this.getCreator().equals(other.getCreator()))
            && (this.getImportConfig() == null ? other.getImportConfig() == null : this.getImportConfig().equals(other.getImportConfig()))
            && (this.getSourceInfo() == null ? other.getSourceInfo() == null : this.getSourceInfo().equals(other.getSourceInfo()))
            && (this.getSourceType() == null ? other.getSourceType() == null : this.getSourceType().equals(other.getSourceType()))
            && (this.getClient() == null ? other.getClient() == null : this.getClient().equals(other.getClient()))
            && (this.getOffset() == null ? other.getOffset() == null : this.getOffset().equals(other.getOffset()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getEnable() == null ? other.getEnable() == null : this.getEnable().equals(other.getEnable()))
            && (this.getAppName() == null ? other.getAppName() == null : this.getAppName().equals(other.getAppName()))
            && (this.getUserImportConfig() == null ? other.getUserImportConfig() == null : this.getUserImportConfig().equals(other.getUserImportConfig()))
            && (this.getEffectiveThreshold() == null ? other.getEffectiveThreshold() == null : this.getEffectiveThreshold().equals(other.getEffectiveThreshold()))
            && (this.getNotifiers() == null ? other.getNotifiers() == null : this.getNotifiers().equals(other.getNotifiers()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getModifier() == null) ? 0 : getModifier().hashCode());
        result = prime * result + ((getCreator() == null) ? 0 : getCreator().hashCode());
        result = prime * result + ((getImportConfig() == null) ? 0 : getImportConfig().hashCode());
        result = prime * result + ((getSourceInfo() == null) ? 0 : getSourceInfo().hashCode());
        result = prime * result + ((getSourceType() == null) ? 0 : getSourceType().hashCode());
        result = prime * result + ((getClient() == null) ? 0 : getClient().hashCode());
        result = prime * result + ((getOffset() == null) ? 0 : getOffset().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getEnable() == null) ? 0 : getEnable().hashCode());
        result = prime * result + ((getAppName() == null) ? 0 : getAppName().hashCode());
        result = prime * result + ((getUserImportConfig() == null) ? 0 : getUserImportConfig().hashCode());
        result = prime * result + ((getEffectiveThreshold() == null) ? 0 : getEffectiveThreshold().hashCode());
        result = prime * result + ((getNotifiers() == null) ? 0 : getNotifiers().hashCode());
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
        sb.append(", modifier=").append(modifier);
        sb.append(", creator=").append(creator);
        sb.append(", importConfig=").append(importConfig);
        sb.append(", sourceInfo=").append(sourceInfo);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", client=").append(client);
        sb.append(", offset=").append(offset);
        sb.append(", status=").append(status);
        sb.append(", name=").append(name);
        sb.append(", enable=").append(enable);
        sb.append(", appName=").append(appName);
        sb.append(", userImportConfig=").append(userImportConfig);
        sb.append(", effectiveThreshold=").append(effectiveThreshold);
        sb.append(", notifiers=").append(notifiers);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}