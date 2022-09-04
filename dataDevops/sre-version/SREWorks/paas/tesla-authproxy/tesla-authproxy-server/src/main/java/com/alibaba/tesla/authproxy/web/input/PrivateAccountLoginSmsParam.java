package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 登录短信验证码发送 API
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountLoginSmsParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.aliyunId}")
    private String aliyunId;

    @NotEmpty(message = "{private.validation.required.password}")
    private String password;

    public void cleanSelf() {
        this.aliyunId = aliyunId.trim();
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
