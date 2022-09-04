package com.alibaba.sreworks.warehouse.domain;

import java.io.Serializable;
import java.util.Date;

public class SwEntity implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String alias;

    private String tableName;

    private String tableAlias;

    private Boolean buildIn;

    private String layer;

    private String partitionFormat;

    private Integer lifecycle;

    private String icon;

    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName == null ? null : tableName.trim();
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias == null ? null : tableAlias.trim();
    }

    public Boolean getBuildIn() {
        return buildIn;
    }

    public void setBuildIn(Boolean buildIn) {
        this.buildIn = buildIn;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer == null ? null : layer.trim();
    }

    public String getPartitionFormat() {
        return partitionFormat;
    }

    public void setPartitionFormat(String partitionFormat) {
        this.partitionFormat = partitionFormat == null ? null : partitionFormat.trim();
    }

    public Integer getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Integer lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon == null ? null : icon.trim();
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
        SwEntity other = (SwEntity) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getAlias() == null ? other.getAlias() == null : this.getAlias().equals(other.getAlias()))
            && (this.getTableName() == null ? other.getTableName() == null : this.getTableName().equals(other.getTableName()))
            && (this.getTableAlias() == null ? other.getTableAlias() == null : this.getTableAlias().equals(other.getTableAlias()))
            && (this.getBuildIn() == null ? other.getBuildIn() == null : this.getBuildIn().equals(other.getBuildIn()))
            && (this.getLayer() == null ? other.getLayer() == null : this.getLayer().equals(other.getLayer()))
            && (this.getPartitionFormat() == null ? other.getPartitionFormat() == null : this.getPartitionFormat().equals(other.getPartitionFormat()))
            && (this.getLifecycle() == null ? other.getLifecycle() == null : this.getLifecycle().equals(other.getLifecycle()))
            && (this.getIcon() == null ? other.getIcon() == null : this.getIcon().equals(other.getIcon()))
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
        result = prime * result + ((getAlias() == null) ? 0 : getAlias().hashCode());
        result = prime * result + ((getTableName() == null) ? 0 : getTableName().hashCode());
        result = prime * result + ((getTableAlias() == null) ? 0 : getTableAlias().hashCode());
        result = prime * result + ((getBuildIn() == null) ? 0 : getBuildIn().hashCode());
        result = prime * result + ((getLayer() == null) ? 0 : getLayer().hashCode());
        result = prime * result + ((getPartitionFormat() == null) ? 0 : getPartitionFormat().hashCode());
        result = prime * result + ((getLifecycle() == null) ? 0 : getLifecycle().hashCode());
        result = prime * result + ((getIcon() == null) ? 0 : getIcon().hashCode());
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
        sb.append(", alias=").append(alias);
        sb.append(", tableName=").append(tableName);
        sb.append(", tableAlias=").append(tableAlias);
        sb.append(", buildIn=").append(buildIn);
        sb.append(", layer=").append(layer);
        sb.append(", partitionFormat=").append(partitionFormat);
        sb.append(", lifecycle=").append(lifecycle);
        sb.append(", icon=").append(icon);
        sb.append(", description=").append(description);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}