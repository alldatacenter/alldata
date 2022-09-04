package com.alibaba.tesla.action.constant;

import java.io.Serializable;

/**
 * <p>Description:  <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/12/27 上午11:15
 */
public class TeslaAuth implements Serializable {

    public static final String X_AUTH_APP = "x-auth-app";
    public static final String X_AUTH_KEY = "x-auth-key";
    public static final String X_AUTH_USER = "x-auth-user";
    public static final String X_EMPID = "x-empid";
    public static final String X_AUTH_PASSWD = "x-auth-passwd";

    public static final String DEFAULT_AUTH_APP = "teslaweb";
    public static final String DEFAULT_AUTH_KEY = "b2cbbe79-b832-4b79-aabc-6471c1842e32";
    public static final String DEFAULT_AUTH_USER = "yaoxing.gyx";
    public static final String DEFAULT_AUTH_EMPID = "122592";
    public static final String DEFAULT_AUTH_PWD = "com.alibaba.tesla.common.utils.TeslaAuth";

    /**
     * 默认认证
     */
    public static final TeslaAuth DEFAULT_AUTH = new TeslaAuth(DEFAULT_AUTH_APP, DEFAULT_AUTH_KEY, DEFAULT_AUTH_USER, DEFAULT_AUTH_EMPID, DEFAULT_AUTH_PWD);

    String authApp;

    String authKey;

    String authUser;

    String authEmpId;

    String authPasswd;

    public TeslaAuth(){}

    public TeslaAuth(String authApp, String authKey, String authUser, String authEmpId, String authPasswd){
        this.authApp = authApp;
        this.authKey = authKey;
        this.authUser = authUser;
        this.authPasswd = authPasswd;
        this.authEmpId = authEmpId;
    }

    public TeslaAuth(String authApp, String authKey, String authUser, String authPasswd){
        this.authApp = authApp;
        this.authKey = authKey;
        this.authUser = authUser;
        this.authPasswd = authPasswd;
    }

    public String getAuthEmpId() {
        return authEmpId;
    }

    public void setAuthEmpId(String authEmpId) {
        this.authEmpId = authEmpId;
    }

    public String getAuthApp() {
        return authApp;
    }

    public void setAuthApp(String authApp) {
        this.authApp = authApp;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getAuthUser() {
        return authUser;
    }

    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }

    public String getAuthPasswd() {
        return authPasswd;
    }

    public void setAuthPasswd(String authPasswd) {
        this.authPasswd = authPasswd;
    }
}
