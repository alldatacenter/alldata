package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 切换语言 API 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class TeslaUserLangParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.lang}")
    private String lang;

    public void cleanSelf() {
        this.lang = lang.trim();
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
