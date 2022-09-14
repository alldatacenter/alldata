package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;

/**
 * 登录短信发送返回结构
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountLoginSmsResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String aliyunId;

    private String phone;

    public String getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(String aliyunId) {
        this.aliyunId = aliyunId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

}
