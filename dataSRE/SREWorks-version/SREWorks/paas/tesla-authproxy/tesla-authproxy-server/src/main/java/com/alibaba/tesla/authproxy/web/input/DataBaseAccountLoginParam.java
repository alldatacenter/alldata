package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 专有云 - 账户登录
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DataBaseAccountLoginParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{database.validation.required.loginName}")
    private String loginName;

    @NotEmpty(message = "{database.validation.required.password}")
    private String password;

    private String smsCode = "";

    private String lang = "zh_CN";

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void cleanSelf() {
        this.loginName = loginName.trim();
        this.smsCode = smsCode.trim();
        this.lang = lang.trim();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
