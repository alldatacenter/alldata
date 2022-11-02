package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;

/**
 * 修改语言的返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class TeslaUserLangResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String aliyunId;

    private String lang;

    public String getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(String aliyunId) {
        this.aliyunId = aliyunId;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

}
