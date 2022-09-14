package com.alibaba.tesla.gateway.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 登录用户信息
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class LoginUserInfo implements Serializable {

    private String accessKeyId;

    private String accessKeySecret;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private String aliww;

    private String aliyunPk;

    private List<Map<String, String>> authorities;

    private String bid;

    private boolean credentialsNonExpired;

    private String dingding;

    private String email;

    private String empId;

    private boolean enabled;

    private String gmtCreate;

    private String gmtModified;

    private int id;

    private int isFirstLogin;

    private int isImmutable;

    private int isLocked;

    private String lang;

    private String lastLoginTime;

    private String loginName;

    private String userId;

    private String memo;

    private String nickName;

    private String phone;

    private String secretKey;

    private int status;

    private String appId;

    private boolean checkSuccess;
}
