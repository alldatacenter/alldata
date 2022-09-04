package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
public class CustomAddonInstanceDO implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String addonInstanceId;

    private String namespaceId;

    private String addonId;

    private String addonName;

    private String addonVersion;

    private String addonAttrs;

    private String addonExt;

    private String dataOutput;

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

    public String getAddonInstanceId() {
        return addonInstanceId;
    }

    public void setAddonInstanceId(String addonInstanceId) {
        this.addonInstanceId = addonInstanceId == null ? null : addonInstanceId.trim();
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

    public String getAddonExt() {
        return addonExt;
    }

    public void setAddonExt(String addonExt) {
        this.addonExt = addonExt == null ? null : addonExt.trim();
    }

    public String getDataOutput() {
        return dataOutput;
    }

    public void setDataOutput(String dataOutput) {
        this.dataOutput = dataOutput == null ? null : dataOutput.trim();
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
        CustomAddonInstanceDO other = (CustomAddonInstanceDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
                && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
                && (this.getAddonInstanceId() == null ? other.getAddonInstanceId() == null : this.getAddonInstanceId().equals(other.getAddonInstanceId()))
                && (this.getNamespaceId() == null ? other.getNamespaceId() == null : this.getNamespaceId().equals(other.getNamespaceId()))
                && (this.getAddonId() == null ? other.getAddonId() == null : this.getAddonId().equals(other.getAddonId()))
                && (this.getAddonName() == null ? other.getAddonName() == null : this.getAddonName().equals(other.getAddonName()))
                && (this.getAddonVersion() == null ? other.getAddonVersion() == null : this.getAddonVersion().equals(other.getAddonVersion()))
                && (this.getAddonAttrs() == null ? other.getAddonAttrs() == null : this.getAddonAttrs().equals(other.getAddonAttrs()))
                && (this.getAddonExt() == null ? other.getAddonExt() == null : this.getAddonExt().equals(other.getAddonExt()))
                && (this.getDataOutput() == null ? other.getDataOutput() == null : this.getDataOutput().equals(other.getDataOutput()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getAddonInstanceId() == null) ? 0 : getAddonInstanceId().hashCode());
        result = prime * result + ((getNamespaceId() == null) ? 0 : getNamespaceId().hashCode());
        result = prime * result + ((getAddonId() == null) ? 0 : getAddonId().hashCode());
        result = prime * result + ((getAddonName() == null) ? 0 : getAddonName().hashCode());
        result = prime * result + ((getAddonVersion() == null) ? 0 : getAddonVersion().hashCode());
        result = prime * result + ((getAddonAttrs() == null) ? 0 : getAddonAttrs().hashCode());
        result = prime * result + ((getAddonExt() == null) ? 0 : getAddonExt().hashCode());
        result = prime * result + ((getDataOutput() == null) ? 0 : getDataOutput().hashCode());
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
        sb.append(", addonInstanceId=").append(addonInstanceId);
        sb.append(", namespaceId=").append(namespaceId);
        sb.append(", addonId=").append(addonId);
        sb.append(", addonName=").append(addonName);
        sb.append(", addonVersion=").append(addonVersion);
        sb.append(", addonAttrs=").append(addonAttrs);
        sb.append(", addonExt=").append(addonExt);
        sb.append(", dataOutput=").append(dataOutput);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}