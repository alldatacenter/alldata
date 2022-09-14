package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 云账号列表修改验证方式 API 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountValidationParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.validation}")
    private String validation;

    public void cleanSelf() {
        this.validation = validation.trim();
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
