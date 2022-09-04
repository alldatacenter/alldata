package com.alibaba.sreworks.dataset.domain.primary;

import java.io.Serializable;

public class DataModelConfigWithBLOBs extends DataModelConfig implements Serializable {
    private String query;

    private String description;

    private static final long serialVersionUID = 1L;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? null : query.trim();
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
        DataModelConfigWithBLOBs other = (DataModelConfigWithBLOBs) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getLabel() == null ? other.getLabel() == null : this.getLabel().equals(other.getLabel()))
            && (this.getBuildIn() == null ? other.getBuildIn() == null : this.getBuildIn().equals(other.getBuildIn()))
            && (this.getDomainId() == null ? other.getDomainId() == null : this.getDomainId().equals(other.getDomainId()))
            && (this.getTeamId() == null ? other.getTeamId() == null : this.getTeamId().equals(other.getTeamId()))
            && (this.getSourceType() == null ? other.getSourceType() == null : this.getSourceType().equals(other.getSourceType()))
            && (this.getSourceId() == null ? other.getSourceId() == null : this.getSourceId().equals(other.getSourceId()))
            && (this.getSourceTable() == null ? other.getSourceTable() == null : this.getSourceTable().equals(other.getSourceTable()))
            && (this.getGranularity() == null ? other.getGranularity() == null : this.getGranularity().equals(other.getGranularity()))
            && (this.getQueryFields() == null ? other.getQueryFields() == null : this.getQueryFields().equals(other.getQueryFields()))
            && (this.getValueFields() == null ? other.getValueFields() == null : this.getValueFields().equals(other.getValueFields()))
            && (this.getGroupFields() == null ? other.getGroupFields() == null : this.getGroupFields().equals(other.getGroupFields()))
            && (this.getModelFields() == null ? other.getModelFields() == null : this.getModelFields().equals(other.getModelFields()))
            && (this.getQuery() == null ? other.getQuery() == null : this.getQuery().equals(other.getQuery()))
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
        result = prime * result + ((getLabel() == null) ? 0 : getLabel().hashCode());
        result = prime * result + ((getBuildIn() == null) ? 0 : getBuildIn().hashCode());
        result = prime * result + ((getDomainId() == null) ? 0 : getDomainId().hashCode());
        result = prime * result + ((getTeamId() == null) ? 0 : getTeamId().hashCode());
        result = prime * result + ((getSourceType() == null) ? 0 : getSourceType().hashCode());
        result = prime * result + ((getSourceId() == null) ? 0 : getSourceId().hashCode());
        result = prime * result + ((getSourceTable() == null) ? 0 : getSourceTable().hashCode());
        result = prime * result + ((getGranularity() == null) ? 0 : getGranularity().hashCode());
        result = prime * result + ((getQueryFields() == null) ? 0 : getQueryFields().hashCode());
        result = prime * result + ((getValueFields() == null) ? 0 : getValueFields().hashCode());
        result = prime * result + ((getGroupFields() == null) ? 0 : getGroupFields().hashCode());
        result = prime * result + ((getModelFields() == null) ? 0 : getModelFields().hashCode());
        result = prime * result + ((getQuery() == null) ? 0 : getQuery().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", query=").append(query);
        sb.append(", description=").append(description);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}