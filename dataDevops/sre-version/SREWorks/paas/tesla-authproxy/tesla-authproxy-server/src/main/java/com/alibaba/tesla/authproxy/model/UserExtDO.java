package com.alibaba.tesla.authproxy.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户扩展信息
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
public class UserExtDO implements Serializable {

    private String depId;
    private String businessUnit;
    private String supervisorEmail;
    private String supervisorEmpId;
    private String supervisorName;
    private String nickName;
    private String nickNameCn;
    private String fromEmpId;
    private String fromLoginName;
    private String fromBucId;
    private Boolean canSwitchView;
    private String avatar;

    public UserExtDO() {
        this.depId = "";
        this.businessUnit = "";
        this.supervisorEmail = "";
        this.supervisorEmpId = "";
        this.supervisorName = "";
        this.nickName = "";
        this.nickNameCn = "";
        this.fromEmpId = "";
        this.fromLoginName = "";
        this.fromBucId = "";
        this.canSwitchView = false;
        this.avatar = "";
    }
}
