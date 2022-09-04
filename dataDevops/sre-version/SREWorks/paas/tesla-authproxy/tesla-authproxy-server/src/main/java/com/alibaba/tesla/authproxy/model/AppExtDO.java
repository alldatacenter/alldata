package com.alibaba.tesla.authproxy.model;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>Description: 外部应用信息 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class AppExtDO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String extAppName;
    private String extAppKey;
    private String memo;
    private Date gmtCreate;
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExtAppName() {
        return extAppName;
    }

    public void setExtAppName(String extAppName) {
        this.extAppName = extAppName == null ? null : extAppName.trim();
    }

    public String getExtAppKey() {
        return extAppKey;
    }

    public void setExtAppKey(String extAppKey) {
        this.extAppKey = extAppKey == null ? null : extAppKey.trim();
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? null : memo.trim();
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
        AppExtDO other = (AppExtDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getExtAppName() == null ? other.getExtAppName() == null : this.getExtAppName().equals(other.getExtAppName()))
                && (this.getExtAppKey() == null ? other.getExtAppKey() == null : this.getExtAppKey().equals(other.getExtAppKey()))
                && (this.getMemo() == null ? other.getMemo() == null : this.getMemo().equals(other.getMemo()))
                && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
                && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getExtAppName() == null) ? 0 : getExtAppName().hashCode());
        result = prime * result + ((getExtAppKey() == null) ? 0 : getExtAppKey().hashCode());
        result = prime * result + ((getMemo() == null) ? 0 : getMemo().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", extAppName=").append(extAppName);
        sb.append(", extAppKey=").append(extAppKey);
        sb.append(", memo=").append(memo);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}