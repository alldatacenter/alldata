package com.platform.system.dcJobConfig.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.platform.common.aspectj.lang.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 岗位表 system_dc_job_config
 * 
 * @author AllDataDC
 */
public class Jobconfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Excel(name = "id", cellType = Excel.ColumnType.NUMERIC)
    private Long id;

    @Excel(name = "originTableName")
    private String originTableName;

    @Excel(name = "originTablePrimary")
    private String originTablePrimary;

    @Excel(name = "originTableFields")
    private String originTableFields;

    @Excel(name = "toTableName")
    private String toTableName;

    @Excel(name = "toTablePrimary")
    private String toTablePrimary;

    @Excel(name = "toTableFields")
    private String toTableFields;

    /** createBy */
    @Excel(name = "createBy")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "dbConfigId")
    private Long dbConfigId;

    @Excel(name = "schduleTime")
    private String schduleTime;

    @Excel(name = "schduleStatus")
    private String schduleStatus;

    @Excel(name = "originTableFilter")
    private String originTableFilter;

    @Excel(name = "toTableFilter")
    private String toTableFilter;

    @Excel(name = "originTableGroup")
    private String originTableGroup;

    @Excel(name = "toTableGroup")
    private String toTableGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginTableName() {
        return originTableName;
    }

    public void setOriginTableName(String originTableName) {
        this.originTableName = originTableName;
    }

    public String getOriginTablePrimary() {
        return originTablePrimary;
    }

    public void setOriginTablePrimary(String originTablePrimary) {
        this.originTablePrimary = originTablePrimary;
    }

    public String getOriginTableFields() {
        return originTableFields;
    }

    public void setOriginTableFields(String originTableFields) {
        this.originTableFields = originTableFields;
    }

    public String getToTableName() {
        return toTableName;
    }

    public void setToTableName(String toTableName) {
        this.toTableName = toTableName;
    }

    public String getToTablePrimary() {
        return toTablePrimary;
    }

    public void setToTablePrimary(String toTablePrimary) {
        this.toTablePrimary = toTablePrimary;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getToTableFields() {
        return toTableFields;
    }

    public void setToTableFields(String toTableFields) {
        this.toTableFields = toTableFields;
    }

    public String getSchduleTime() {
        return schduleTime;
    }

    public void setSchduleTime(String schduleTime) {
        this.schduleTime = schduleTime;
    }

    public String getSchduleStatus() {
        return schduleStatus;
    }

    public void setSchduleStatus(String schduleStatus) {
        this.schduleStatus = schduleStatus;
    }

    public Long getDbConfigId() {
        return dbConfigId;
    }

    public void setDbConfigId(Long dbConfigId) {
        this.dbConfigId = dbConfigId;
    }


    public String getOriginTableFilter() {
        return originTableFilter;
    }

    public void setOriginTableFilter(String originTableFilter) {
        this.originTableFilter = originTableFilter;
    }

    public String getToTableFilter() {
        return toTableFilter;
    }

    public void setToTableFilter(String toTableFilter) {
        this.toTableFilter = toTableFilter;
    }

    public String getOriginTableGroup() {
        return originTableGroup;
    }

    public void setOriginTableGroup(String originTableGroup) {
        this.originTableGroup = originTableGroup;
    }

    public String getToTableGroup() {
        return toTableGroup;
    }

    public void setToTableGroup(String toTableGroup) {
        this.toTableGroup = toTableGroup;
    }
}
