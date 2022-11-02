package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录返回数据结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DataBaseLoginResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String loginName;

    private String email;

    private String empId;

    private String nickName;

    private Date passwordChangeTime;

    private Boolean passwordChangeNotify;

    private Integer passwordChangeRestDays;

    private Boolean isFirstLogin;

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
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

    public Boolean getFirstLogin() {
        return isFirstLogin;
    }

    public void setFirstLogin(Boolean firstLogin) {
        isFirstLogin = firstLogin;
    }
}
