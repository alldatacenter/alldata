package com.platform.system.dcDbConfig.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.platform.common.aspectj.lang.annotation.Excel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * 岗位表 system_dc_db_config
 * 
 * @author AllDataDC
 */
public class Dbconfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** id */
    @Excel(name = "id", cellType = Excel.ColumnType.NUMERIC)
    private Long id;

    /** 数据库连接名称 */
    @Excel(name = "数据库连接名称")
    private String connectName;

    /** 数据库类型 */
    @Excel(name = "数据库类型")
    private String type;

    /** url */
    @Excel(name = "url")
    private String url;

    /** user */
    @Excel(name = "userName")
    private String userName;

    /** pwd */
    @Excel(name = "pwd")
    private String pwd;

    /** createBy */
    @Excel(name = "createBy")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "连接名称不能为空")
    public String getConnectName()
    {
        return connectName;
    }

    public void setConnectName(String connectName)
    {
        this.connectName = connectName;
    }

    @NotBlank(message = "url不能为空")
    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    @NotBlank(message = "userName不能为空")
    public String getUserName()
    {
        return userName;
    }

    public boolean flag = false;

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getPwd()
    {
        return pwd;
    }

    public void setPwd(String pwd)
    {
        this.pwd = pwd;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("connectName", getConnectName())
            .append("type",getType())
            .append("url", getUrl())
            .append("userName", getUserName())
            .append("pwd", getPwd())
            .append("createBy",getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
