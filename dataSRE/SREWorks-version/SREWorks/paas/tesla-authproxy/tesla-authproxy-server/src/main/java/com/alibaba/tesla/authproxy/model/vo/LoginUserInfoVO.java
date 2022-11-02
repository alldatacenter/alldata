package com.alibaba.tesla.authproxy.model.vo;

/**
 * <p>Description: 用户登录信息值对象 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class LoginUserInfoVO {

    private String loginName;

    private Integer bucId;

    private String empId;

    private long aliyunPk;

    private String loginTicket;

    private String bid;

    /**
     * 登录失败后，跳转到登录页面
     */
    private String loginUrl;

    private String userName;

    private String email;

    private String nickName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getEmpId() {
        return this.empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLoginName() {
        return this.loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public Integer getBucId() {
        return this.bucId;
    }

    public void setBucId(Integer bucId) {
        this.bucId = bucId;
    }

    public long getAliyunPk() {
        return this.aliyunPk;
    }

    public void setAliyunPk(long aliyunPk) {
        this.aliyunPk = aliyunPk;
    }

    public String getLoginTicket() {
        return this.loginTicket;
    }

    public void setLoginTicket(String loginTicket) {
        this.loginTicket = loginTicket;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }
}
