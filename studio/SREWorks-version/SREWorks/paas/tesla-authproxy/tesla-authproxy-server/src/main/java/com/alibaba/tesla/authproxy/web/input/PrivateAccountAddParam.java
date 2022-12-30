package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 云账号列表新建 API 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountAddParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.aliyunId}")
    private String aliyunId;

    @NotEmpty(message = "{private.validation.required.password}")
    private String password;

    @NotEmpty(message = "{private.validation.required.phone}")
    private String phone;

    @NotNull(message = "{private.validation.required.passwordChangeRequired}")
    private Boolean passwordChangeRequired;

    public void cleanSelf() {
        this.aliyunId = aliyunId.trim();
        this.phone = phone.trim();
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(Boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
