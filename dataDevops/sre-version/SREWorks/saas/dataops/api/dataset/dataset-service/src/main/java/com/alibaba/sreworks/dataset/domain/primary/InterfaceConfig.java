package com.alibaba.sreworks.dataset.domain.primary;

import java.io.Serializable;
import java.util.Date;

public class InterfaceConfig implements Serializable {
    private Integer id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String alias;

    private String dataSourceType;

    private String dataSourceId;

    private String dataSourceTable;

    private String mode;

    private String queryFields;

    private String groupFields;

    private String sortFields;

    private String requestParams;

    private String responseParams;

    private Boolean buildIn;

    private String creator;

    private String lastModifier;

    private String requestMethod;

    private String contentType;

    private Boolean paging;

    private String qlTemplate;

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType == null ? null : dataSourceType.trim();
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId == null ? null : dataSourceId.trim();
    }

    public String getDataSourceTable() {
        return dataSourceTable;
    }

    public void setDataSourceTable(String dataSourceTable) {
        this.dataSourceTable = dataSourceTable == null ? null : dataSourceTable.trim();
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode == null ? null : mode.trim();
    }

    public String getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(String queryFields) {
        this.queryFields = queryFields == null ? null : queryFields.trim();
    }

    public String getGroupFields() {
        return groupFields;
    }

    public void setGroupFields(String groupFields) {
        this.groupFields = groupFields == null ? null : groupFields.trim();
    }

    public String getSortFields() {
        return sortFields;
    }

    public void setSortFields(String sortFields) {
        this.sortFields = sortFields == null ? null : sortFields.trim();
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams == null ? null : requestParams.trim();
    }

    public String getResponseParams() {
        return responseParams;
    }

    public void setResponseParams(String responseParams) {
        this.responseParams = responseParams == null ? null : responseParams.trim();
    }

    public Boolean getBuildIn() {
        return buildIn;
    }

    public void setBuildIn(Boolean buildIn) {
        this.buildIn = buildIn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator == null ? null : creator.trim();
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier == null ? null : lastModifier.trim();
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod == null ? null : requestMethod.trim();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType == null ? null : contentType.trim();
    }

    public Boolean getPaging() {
        return paging;
    }

    public void setPaging(Boolean paging) {
        this.paging = paging;
    }

    public String getQlTemplate() {
        return qlTemplate;
    }

    public void setQlTemplate(String qlTemplate) {
        this.qlTemplate = qlTemplate == null ? null : qlTemplate.trim();
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
        InterfaceConfig other = (InterfaceConfig) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getAlias() == null ? other.getAlias() == null : this.getAlias().equals(other.getAlias()))
            && (this.getDataSourceType() == null ? other.getDataSourceType() == null : this.getDataSourceType().equals(other.getDataSourceType()))
            && (this.getDataSourceId() == null ? other.getDataSourceId() == null : this.getDataSourceId().equals(other.getDataSourceId()))
            && (this.getDataSourceTable() == null ? other.getDataSourceTable() == null : this.getDataSourceTable().equals(other.getDataSourceTable()))
            && (this.getMode() == null ? other.getMode() == null : this.getMode().equals(other.getMode()))
            && (this.getQueryFields() == null ? other.getQueryFields() == null : this.getQueryFields().equals(other.getQueryFields()))
            && (this.getGroupFields() == null ? other.getGroupFields() == null : this.getGroupFields().equals(other.getGroupFields()))
            && (this.getSortFields() == null ? other.getSortFields() == null : this.getSortFields().equals(other.getSortFields()))
            && (this.getRequestParams() == null ? other.getRequestParams() == null : this.getRequestParams().equals(other.getRequestParams()))
            && (this.getResponseParams() == null ? other.getResponseParams() == null : this.getResponseParams().equals(other.getResponseParams()))
            && (this.getBuildIn() == null ? other.getBuildIn() == null : this.getBuildIn().equals(other.getBuildIn()))
            && (this.getCreator() == null ? other.getCreator() == null : this.getCreator().equals(other.getCreator()))
            && (this.getLastModifier() == null ? other.getLastModifier() == null : this.getLastModifier().equals(other.getLastModifier()))
            && (this.getRequestMethod() == null ? other.getRequestMethod() == null : this.getRequestMethod().equals(other.getRequestMethod()))
            && (this.getContentType() == null ? other.getContentType() == null : this.getContentType().equals(other.getContentType()))
            && (this.getPaging() == null ? other.getPaging() == null : this.getPaging().equals(other.getPaging()))
            && (this.getQlTemplate() == null ? other.getQlTemplate() == null : this.getQlTemplate().equals(other.getQlTemplate()));
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
        result = prime * result + ((getDataSourceType() == null) ? 0 : getDataSourceType().hashCode());
        result = prime * result + ((getDataSourceId() == null) ? 0 : getDataSourceId().hashCode());
        result = prime * result + ((getDataSourceTable() == null) ? 0 : getDataSourceTable().hashCode());
        result = prime * result + ((getMode() == null) ? 0 : getMode().hashCode());
        result = prime * result + ((getQueryFields() == null) ? 0 : getQueryFields().hashCode());
        result = prime * result + ((getGroupFields() == null) ? 0 : getGroupFields().hashCode());
        result = prime * result + ((getSortFields() == null) ? 0 : getSortFields().hashCode());
        result = prime * result + ((getRequestParams() == null) ? 0 : getRequestParams().hashCode());
        result = prime * result + ((getResponseParams() == null) ? 0 : getResponseParams().hashCode());
        result = prime * result + ((getBuildIn() == null) ? 0 : getBuildIn().hashCode());
        result = prime * result + ((getCreator() == null) ? 0 : getCreator().hashCode());
        result = prime * result + ((getLastModifier() == null) ? 0 : getLastModifier().hashCode());
        result = prime * result + ((getRequestMethod() == null) ? 0 : getRequestMethod().hashCode());
        result = prime * result + ((getContentType() == null) ? 0 : getContentType().hashCode());
        result = prime * result + ((getPaging() == null) ? 0 : getPaging().hashCode());
        result = prime * result + ((getQlTemplate() == null) ? 0 : getQlTemplate().hashCode());
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
        sb.append(", dataSourceType=").append(dataSourceType);
        sb.append(", dataSourceId=").append(dataSourceId);
        sb.append(", dataSourceTable=").append(dataSourceTable);
        sb.append(", mode=").append(mode);
        sb.append(", queryFields=").append(queryFields);
        sb.append(", groupFields=").append(groupFields);
        sb.append(", sortFields=").append(sortFields);
        sb.append(", requestParams=").append(requestParams);
        sb.append(", responseParams=").append(responseParams);
        sb.append(", buildIn=").append(buildIn);
        sb.append(", creator=").append(creator);
        sb.append(", lastModifier=").append(lastModifier);
        sb.append(", requestMethod=").append(requestMethod);
        sb.append(", contentType=").append(contentType);
        sb.append(", paging=").append(paging);
        sb.append(", qlTemplate=").append(qlTemplate);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}