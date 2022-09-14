package com.alibaba.tesla.authproxy.web.output;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * 云账号列表新增 API 返回结构
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountAddResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String aliyunId;

    private String accessKeyId;

    private String accessKeySecret;

    private Date createTime;

    private String phone;

    private Boolean isFirstLogin;

    private Boolean isLock;

    private Date passwordChangeNextTime;

    private Integer passwordChangeRestDays;

    public String getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(String aliyunId) {
        this.aliyunId = aliyunId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getFirstLogin() {
        return isFirstLogin;
    }

    public void setFirstLogin(Boolean firstLogin) {
        isFirstLogin = firstLogin;
    }

    public Boolean getLock() {
        return isLock;
    }

    public void setLock(Boolean lock) {
        isLock = lock;
    }

    public Date getPasswordChangeNextTime() {
        return passwordChangeNextTime;
    }

    public void setPasswordChangeNextTime(Date passwordChangeNextTime) {
        this.passwordChangeNextTime = passwordChangeNextTime;
    }

    public Integer getPasswordChangeRestDays() {
        return passwordChangeRestDays;
    }

    public void setPasswordChangeRestDays(Integer passwordChangeRestDays) {
        this.passwordChangeRestDays = passwordChangeRestDays;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

