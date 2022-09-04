package com.alibaba.tesla.authproxy.web.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 云账号列表获取 API 返回结构
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountListResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String validation;

    private Boolean smsGateway;

    private List<PrivateAccountItem> results;

    public PrivateAccountListResult() {
        this.results = new ArrayList<>();
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public List<PrivateAccountItem> getResults() {
        return results;
    }

    public void addResult(PrivateAccountItem item) {
        this.results.add(item);
    }

    public Boolean getSmsGateway() {
        return smsGateway;
    }

    public void setSmsGateway(Boolean smsGateway) {
        this.smsGateway = smsGateway;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonInclude(Include.NON_NULL)
    public static class PrivateAccountItem implements Serializable {

        public static final long serialVersionUID = 1L;

        private String aliyunId;

        private String password;

        private String accessKeyId;

        private String accessKeySecret;

        private Date createTime;

        private String phone;

        private Boolean isFirstLogin;

        private Boolean isLock;

        private Date passwordChangeNextTime;

        private Integer passwordChangeRestDays;

        private Boolean isImmutable;

        public String getAliyunId() {
            return aliyunId;
        }

        public void setAliyunId(String aliyunId) {
            this.aliyunId = aliyunId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
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

        public Boolean getIsLock() {
            return isLock;
        }

        public void setIsLock(Boolean isLock) {
            this.isLock = isLock;
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

        public Boolean getIsImmutable() {
            return isImmutable;
        }

        public void setIsImmutable(Boolean isImmutable) {
            this.isImmutable = isImmutable;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

}

