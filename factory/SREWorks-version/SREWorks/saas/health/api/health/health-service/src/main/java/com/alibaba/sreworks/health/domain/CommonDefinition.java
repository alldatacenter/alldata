package com.alibaba.sreworks.health.domain;

import java.io.Serializable;
import java.util.Date;

public class CommonDefinition implements Serializable {
    private Integer id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String category;

    private String appId;

    private String appName;

    private String appComponentName;

    private Integer metricId;

    private Integer failureRefIncidentId;

    private String creator;

    private String receivers;

    private String lastModifier;

    private String exConfig;

    private String description;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? null : appId.trim();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    public String getAppComponentName() {
        return appComponentName;
    }

    public void setAppComponentName(String appComponentName) {
        this.appComponentName = appComponentName == null ? null : appComponentName.trim();
    }

    public Integer getMetricId() {
        return metricId;
    }

    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    public Integer getFailureRefIncidentId() {
        return failureRefIncidentId;
    }

    public void setFailureRefIncidentId(Integer failureRefIncidentId) {
        this.failureRefIncidentId = failureRefIncidentId;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getReceivers() {
        return receivers;
    }

    public void setReceivers(String receivers) {
        this.receivers = receivers == null ? null : receivers.trim();
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier == null ? null : lastModifier.trim();
    }

    public String getExConfig() {
        return exConfig;
    }

    public void setExConfig(String exConfig) {
        this.exConfig = exConfig == null ? null : exConfig.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
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
        CommonDefinition other = (CommonDefinition) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getAppId() == null ? other.getAppId() == null : this.getAppId().equals(other.getAppId()))
            && (this.getAppName() == null ? other.getAppName() == null : this.getAppName().equals(other.getAppName()))
            && (this.getAppComponentName() == null ? other.getAppComponentName() == null : this.getAppComponentName().equals(other.getAppComponentName()))
            && (this.getMetricId() == null ? other.getMetricId() == null : this.getMetricId().equals(other.getMetricId()))
            && (this.getFailureRefIncidentId() == null ? other.getFailureRefIncidentId() == null : this.getFailureRefIncidentId().equals(other.getFailureRefIncidentId()))
            && (this.getCreator() == null ? other.getCreator() == null : this.getCreator().equals(other.getCreator()))
            && (this.getReceivers() == null ? other.getReceivers() == null : this.getReceivers().equals(other.getReceivers()))
            && (this.getLastModifier() == null ? other.getLastModifier() == null : this.getLastModifier().equals(other.getLastModifier()))
            && (this.getExConfig() == null ? other.getExConfig() == null : this.getExConfig().equals(other.getExConfig()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getAppId() == null) ? 0 : getAppId().hashCode());
        result = prime * result + ((getAppName() == null) ? 0 : getAppName().hashCode());
        result = prime * result + ((getAppComponentName() == null) ? 0 : getAppComponentName().hashCode());
        result = prime * result + ((getMetricId() == null) ? 0 : getMetricId().hashCode());
        result = prime * result + ((getFailureRefIncidentId() == null) ? 0 : getFailureRefIncidentId().hashCode());
        result = prime * result + ((getCreator() == null) ? 0 : getCreator().hashCode());
        result = prime * result + ((getReceivers() == null) ? 0 : getReceivers().hashCode());
        result = prime * result + ((getLastModifier() == null) ? 0 : getLastModifier().hashCode());
        result = prime * result + ((getExConfig() == null) ? 0 : getExConfig().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
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
        sb.append(", name=").append(name);
        sb.append(", category=").append(category);
        sb.append(", appId=").append(appId);
        sb.append(", appName=").append(appName);
        sb.append(", appComponentName=").append(appComponentName);
        sb.append(", metricId=").append(metricId);
        sb.append(", failureRefIncidentId=").append(failureRefIncidentId);
        sb.append(", creator=").append(creator);
        sb.append(", receivers=").append(receivers);
        sb.append(", lastModifier=").append(lastModifier);
        sb.append(", exConfig=").append(exConfig);
        sb.append(", description=").append(description);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}