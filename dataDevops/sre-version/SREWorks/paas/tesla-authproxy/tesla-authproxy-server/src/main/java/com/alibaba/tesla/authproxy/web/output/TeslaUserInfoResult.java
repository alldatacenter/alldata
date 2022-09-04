package com.alibaba.tesla.authproxy.web.output;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.UserDO;
import lombok.Data;

import java.util.Date;

@Data
public class TeslaUserInfoResult {

    private Long id;
    private String loginName;
    private String userId;
    private String appId;
    private String aliyunPk;
    private String bid;
    private String empId;
    private String secretKey;
    private String accessKeyId;
    private String accessKeySecret;
    private String aliww;
    private String dingding;
    private String email;
    private Byte isFirstLogin;
    private Byte isImmutable;
    private Byte isLocked;
    private String lang;
    private String memo;
    private String avatar;
    private String nickName;
    private String phone;
    private Integer status;
    private Date lastLoginTime;
    private Date gmtCreate;
    private Date gmtModified;

    public TeslaUserInfoResult(UserDO userDo, String appId, String environment) {
        this.id = userDo.getId();
        this.loginName = userDo.getLoginName();
        if (!Constants.ENVIRONMENT_INTERNAL.equals(environment)) {
            this.userId = userDo.getAliyunPk();
        } else {
            this.userId = String.valueOf(userDo.getBucId());
        }
        this.appId = appId;
        this.aliyunPk = userDo.getAliyunPk();
        this.bid = userDo.getBid();
        this.empId = userDo.getEmpId();
        this.secretKey = userDo.getSecretKey();
        this.accessKeyId = userDo.getAccessKeyId();
        this.accessKeySecret = userDo.getAccessKeySecret();
        this.aliww = userDo.getAliww();
        this.dingding = userDo.getDingding();
        this.email = userDo.getEmail();
        this.isFirstLogin = userDo.getIsFirstLogin();
        this.isImmutable = userDo.getIsImmutable();
        this.isLocked = userDo.getIsLocked();
        this.lang = userDo.getLang();
        this.memo = userDo.getMemo();
        this.avatar = userDo.getAvatar();
        this.nickName = userDo.getNickName();
        this.phone = userDo.getPhone();
        this.status = userDo.getStatus();
        this.lastLoginTime = userDo.getLastLoginTime();
        this.gmtCreate = userDo.getGmtCreate();
        this.gmtModified = userDo.getGmtModified();
    }
}
