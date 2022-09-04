package com.alibaba.tesla.authproxy.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author cdx
 * @date 2019/10/10
 */
@Data
public class TeslaServiceUserDO implements Serializable {
    private Long userid;
    private String username;
    private String employeeId;
    private String bucUserId;
    private String nickname;
    private String nicknamePinyin;
    private String name;
    private String secretkey;
    private String email;
    private String telephone;
    private String mobilephone;
    private String aliww;
    private Integer issuperadmin;
    private Date createtime;
    private Date logintime;
    private Integer validflag;
    private Integer isPublicAccount;
    private String accountSafeguard;
}
