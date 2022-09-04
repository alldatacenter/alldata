package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
public class CustomAddonMetaDO implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String addonId;

    private String addonVersion;

    private String addonType;

    private String addonLabel;

    private String addonSchema;

    private String addonDescription;

    private String addonConfigSchema;

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

    public String getAddonId() {
        return addonId;
    }

    public void setAddonId(String addonId) {
        this.addonId = addonId == null ? null : addonId.trim();
    }

    public String getAddonVersion() {
        return addonVersion;
    }

    public void setAddonVersion(String addonVersion) {
        this.addonVersion = addonVersion == null ? null : addonVersion.trim();
    }

    public String getAddonType() {
        return addonType;
    }

    public void setAddonType(String addonType) {
        this.addonType = addonType == null ? null : addonType.trim();
    }

    public String getAddonLabel() {
        return addonLabel;
    }

    public void setAddonLabel(String addonLabel) {
        this.addonLabel = addonLabel == null ? null : addonLabel.trim();
    }

    public String getAddonSchema() {
        return addonSchema;
    }

    public void setAddonSchema(String addonSchema) {
        this.addonSchema = addonSchema == null ? null : addonSchema.trim();
    }

    public String getAddonDescription() {
        return addonDescription;
    }

    public void setAddonDescription(String addonDescription) {
        this.addonDescription = addonDescription == null ? null : addonDescription.trim();
    }

    public String getAddonConfigSchema() {
        return addonConfigSchema;
    }

    public void setAddonConfigSchema(String addonConfigSchema) {
        this.addonConfigSchema = addonConfigSchema == null ? null : addonConfigSchema.trim();
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
        CustomAddonMetaDO other = (CustomAddonMetaDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
                && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
                && (this.getAddonId() == null ? other.getAddonId() == null : this.getAddonId().equals(other.getAddonId()))
                && (this.getAddonVersion() == null ? other.getAddonVersion() == null : this.getAddonVersion().equals(other.getAddonVersion()))
                && (this.getAddonType() == null ? other.getAddonType() == null : this.getAddonType().equals(other.getAddonType()))
                && (this.getAddonLabel() == null ? other.getAddonLabel() == null : this.getAddonLabel().equals(other.getAddonLabel()))
                && (this.getAddonSchema() == null ? other.getAddonSchema() == null : this.getAddonSchema().equals(other.getAddonSchema()))
                && (this.getAddonDescription() == null ? other.getAddonDescription() == null : this.getAddonDescription().equals(other.getAddonDescription()))
                && (this.getAddonConfigSchema() == null ? other.getAddonConfigSchema() == null : this.getAddonConfigSchema().equals(other.getAddonConfigSchema()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getAddonId() == null) ? 0 : getAddonId().hashCode());
        result = prime * result + ((getAddonVersion() == null) ? 0 : getAddonVersion().hashCode());
        result = prime * result + ((getAddonType() == null) ? 0 : getAddonType().hashCode());
        result = prime * result + ((getAddonLabel() == null) ? 0 : getAddonLabel().hashCode());
        result = prime * result + ((getAddonSchema() == null) ? 0 : getAddonSchema().hashCode());
        result = prime * result + ((getAddonDescription() == null) ? 0 : getAddonDescription().hashCode());
        result = prime * result + ((getAddonConfigSchema() == null) ? 0 : getAddonConfigSchema().hashCode());
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
        sb.append(", addonId=").append(addonId);
        sb.append(", addonVersion=").append(addonVersion);
        sb.append(", addonType=").append(addonType);
        sb.append(", addonLabel=").append(addonLabel);
        sb.append(", addonSchema=").append(addonSchema);
        sb.append(", addonDescription=").append(addonDescription);
        sb.append(", addonConfigSchema=").append(addonConfigSchema);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}