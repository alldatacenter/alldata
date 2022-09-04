package com.alibaba.tesla.authproxy.model;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>Description: 接入权限系统的应用信息 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class AppDO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String appId;
    private String appAccesskey;
    private Date gmtCreate;
    private Date gmtModified;
    /**
     * 默认的应用管理员角色名称
     */
    private String adminRoleName;
    /**
     * 应用的首页URL，用于注销之后再次登录跳转
     */
    private String indexUrl;
    /**
     * 应用的登录地址（固定链接），存在则使用，不存在则自动生成
     */
    private String loginUrl;
    private String memo;
    /**
     * 是否开启登录验证
     * 默认0不开启
     */
    private Integer loginEnable = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotBlank(message = "appId不能为空")
    @Size(max = 45, message = "appId长度必须不能超过45")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? null : appId.trim();
    }

    @NotBlank(message = "appAccesskey不能为空")
    @Size(max = 128, message = "appAccesskey长度必须不能超过128")
    public String getAppAccesskey() {
        return appAccesskey;
    }

    public void setAppAccesskey(String appAccesskey) {
        this.appAccesskey = appAccesskey == null ? null : appAccesskey.trim();
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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? null : memo.trim();
    }


    public String getAdminRoleName() {
        return adminRoleName;
    }

    public void setAdminRoleName(String adminRoleName) {
        this.adminRoleName = adminRoleName;
    }

    @NotBlank(message = "indexUrl不能为空")
    @Size(max = 100, message = "indexUrl长度必须不能超过100")
    public String getIndexUrl() {
        return indexUrl;
    }

    public void setIndexUrl(String indexUrl) {
        this.indexUrl = indexUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public Integer getLoginEnable() {
        return loginEnable;
    }

    public void setLoginEnable(Integer loginEnable) {
        this.loginEnable = loginEnable;
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
        AppDO other = (AppDO) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getAppId() == null ? other.getAppId() == null : this.getAppId().equals(other.getAppId()))
                && (this.getAppAccesskey() == null ? other.getAppAccesskey() == null : this.getAppAccesskey().equals(other.getAppAccesskey()))
                && (this.getAdminRoleName() == null ? other.getAdminRoleName() == null : this.getAdminRoleName().equals(other.getAdminRoleName()))
                && (this.getMemo() == null ? other.getMemo() == null : this.getMemo().equals(other.getMemo()))
                && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
                && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
                && (this.getIndexUrl() == null ? other.getIndexUrl() == null : this.getIndexUrl().equals(other.getIndexUrl()))
                && (this.getLoginUrl() == null ? other.getLoginUrl() == null : this.getLoginUrl().equals(other.getLoginUrl()))
                && (this.getLoginEnable() == null ? other.getLoginEnable() == null : this.getLoginEnable().equals(other.getLoginEnable()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getAppId() == null) ? 0 : getAppId().hashCode());
        result = prime * result + ((getAppAccesskey() == null) ? 0 : getAppAccesskey().hashCode());
        result = prime * result + ((getAdminRoleName() == null) ? 0 : getAdminRoleName().hashCode());
        result = prime * result + ((getMemo() == null) ? 0 : getMemo().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getIndexUrl() == null) ? 0 : getIndexUrl().hashCode());
        result = prime * result + ((getLoginUrl() == null) ? 0 : getLoginUrl().hashCode());
        result = prime * result + ((getLoginEnable() == null) ? 0 : getLoginEnable().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", appId=").append(appId);
        sb.append(", appAccesskey=").append(appAccesskey);
        sb.append(", adminRoleName=").append(adminRoleName);
        sb.append(", memo=").append(memo);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", indexUrl=").append(indexUrl);
        sb.append(", loginEnable=").append(loginEnable);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }

}