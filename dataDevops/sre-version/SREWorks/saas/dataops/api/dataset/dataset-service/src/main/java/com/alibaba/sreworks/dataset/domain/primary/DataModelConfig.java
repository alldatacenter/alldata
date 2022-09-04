package com.alibaba.sreworks.dataset.domain.primary;

import java.io.Serializable;
import java.util.Date;

public class DataModelConfig implements Serializable {
    private Integer id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String label;

    private Boolean buildIn;

    private Integer domainId;

    private Integer teamId;

    private String sourceType;

    private String sourceId;

    private String sourceTable;

    private String granularity;

    private String queryFields;

    private String valueFields;

    private String groupFields;

    private String modelFields;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label == null ? null : label.trim();
    }

    public Boolean getBuildIn() {
        return buildIn;
    }

    public void setBuildIn(Boolean buildIn) {
        this.buildIn = buildIn;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public void setDomainId(Integer domainId) {
        this.domainId = domainId;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType == null ? null : sourceType.trim();
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId == null ? null : sourceId.trim();
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable == null ? null : sourceTable.trim();
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity == null ? null : granularity.trim();
    }

    public String getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(String queryFields) {
        this.queryFields = queryFields == null ? null : queryFields.trim();
    }

    public String getValueFields() {
        return valueFields;
    }

    public void setValueFields(String valueFields) {
        this.valueFields = valueFields == null ? null : valueFields.trim();
    }

    public String getGroupFields() {
        return groupFields;
    }

    public void setGroupFields(String groupFields) {
        this.groupFields = groupFields == null ? null : groupFields.trim();
    }

    public String getModelFields() {
        return modelFields;
    }

    public void setModelFields(String modelFields) {
        this.modelFields = modelFields == null ? null : modelFields.trim();
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
        DataModelConfig other = (DataModelConfig) that;
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
            && (this.getModelFields() == null ? other.getModelFields() == null : this.getModelFields().equals(other.getModelFields()));
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
        sb.append(", label=").append(label);
        sb.append(", buildIn=").append(buildIn);
        sb.append(", domainId=").append(domainId);
        sb.append(", teamId=").append(teamId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceTable=").append(sourceTable);
        sb.append(", granularity=").append(granularity);
        sb.append(", queryFields=").append(queryFields);
        sb.append(", valueFields=").append(valueFields);
        sb.append(", groupFields=").append(groupFields);
        sb.append(", modelFields=").append(modelFields);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}