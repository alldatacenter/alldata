package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;

/**
 * 用户账户类型返回参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountTypeResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String userType;

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
