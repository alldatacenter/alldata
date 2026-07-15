package com.platform.system.dcJobInstance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.platform.common.aspectj.lang.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 岗位表 system_dc_job_instance
 * 
 * @author AllDataDC
 */
public class Instance implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Excel(name = "id", cellType = Excel.ColumnType.NUMERIC)
    private Long id;

    @Excel(name = "jobconfigId")
    private Long jobconfigId;

    @Excel(name = "originTablePv")
    private String originTablePv;

    @Excel(name = "originTableUv")
    private String originTableUv;

    @Excel(name = "toTablePv")
    private String toTablePv;

    @Excel(name = "toTableUv")
    private String toTableUv;

    @Excel(name = "pvDiff")
    private String pvDiff;

    @Excel(name = "uvDiff")
    private String uvDiff;

    @Excel(name = "magnitudeSql")
    private String magnitudeSql;

    @Excel(name = "originTableCount")
    private String originTableCount;

    @Excel(name = "toTableCount")
    private String toTableCount;

    @Excel(name = "countDiff")
    private String countDiff;

    @Excel(name = "consistencySql")
    private String consistencySql;

    @Excel(name = "dt")
    private String dt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String originTableName;

    private String toTableName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobconfigId() {
        return jobconfigId;
    }

    public void setJobconfigId(Long jobconfigId) {
        this.jobconfigId = jobconfigId;
    }

    public String getOriginTablePv() {
        return originTablePv;
    }

    public void setOriginTablePv(String originTablePv) {
        this.originTablePv = originTablePv;
    }

    public String getOriginTableUv() {
        return originTableUv;
    }

    public void setOriginTableUv(String originTableUv) {
        this.originTableUv = originTableUv;
    }

    public String getToTablePv() {
        return toTablePv;
    }

    public void setToTablePv(String toTablePv) {
        this.toTablePv = toTablePv;
    }

    public String getToTableUv() {
        return toTableUv;
    }

    public void setToTableUv(String toTableUv) {
        this.toTableUv = toTableUv;
    }

    public String getPvDiff() {
        return pvDiff;
    }

    public void setPvDiff(String pvDiff) {
        this.pvDiff = pvDiff;
    }

    public String getUvDiff() {
        return uvDiff;
    }

    public void setUvDiff(String uvDiff) {
        this.uvDiff = uvDiff;
    }

    public String getMagnitudeSql() {
        return magnitudeSql;
    }

    public void setMagnitudeSql(String magnitudeSql) {
        this.magnitudeSql = magnitudeSql;
    }

    public String getOriginTableCount() {
        return originTableCount;
    }

    public void setOriginTableCount(String originTableCount) {
        this.originTableCount = originTableCount;
    }

    public String getToTableCount() {
        return toTableCount;
    }

    public void setToTableCount(String toTableCount) {
        this.toTableCount = toTableCount;
    }

    public String getCountDiff() {
        return countDiff;
    }

    public void setCountDiff(String countDiff) {
        this.countDiff = countDiff;
    }

    public String getConsistencySql() {
        return consistencySql;
    }

    public void setConsistencySql(String consistencySql) {
        this.consistencySql = consistencySql;
    }

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getOriginTableName() {
        return originTableName;
    }

    public void setOriginTableName(String originTableName) {
        this.originTableName = originTableName;
    }

    public String getToTableName() {
        return toTableName;
    }

    public void setToTableName(String toTableName) {
        this.toTableName = toTableName;
    }
}
