package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录返回数据结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountLoginResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String aliyunId;

    private Date passwordChangeTime;

    private Boolean passwordChangeNotify;

    private Integer passwordChangeRestDays;

    private Boolean isFirstLogin;

    public String getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(String aliyunId) {
        this.aliyunId = aliyunId;
    }

    public Date getPasswordChangeTime() {
        return passwordChangeTime;
    }

    public void setPasswordChangeTime(Date passwordChangeTime) {
        this.passwordChangeTime = passwordChangeTime;
    }

    public Boolean getPasswordChangeNotify() {
        return passwordChangeNotify;
    }

    public void setPasswordChangeNotify(Boolean passwordChangeNotify) {
        this.passwordChangeNotify = passwordChangeNotify;
    }

    public Integer getPasswordChangeRestDays() {
        return passwordChangeRestDays;
    }

    public void setPasswordChangeRestDays(Integer passwordChangeRestDays) {
        this.passwordChangeRestDays = passwordChangeRestDays;
    }

    public Boolean getIsFirstLogin() {
        return isFirstLogin;
    }

    public void setIsFirstLogin(Boolean isFirstLogin) {
        this.isFirstLogin = isFirstLogin;
    }

}
