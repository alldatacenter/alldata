package com.alibaba.tesla.authproxy.model.vo;

import com.alibaba.tesla.authproxy.model.UserDO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @Date 2019-09-05 14:00
 * @Author cdx
 **/
@Data
@Builder
public class TeslaUserInfoVO {

    private String aliww;
    @JsonProperty("buc_user_id")
    private String bucUserId;
    private Date createtime;
    private String email;
    @JsonProperty("employee_id")
    private String employeeId;
    @JsonProperty("is_public_account")
    private Integer isPublicAccount;
    @JsonProperty("issuperadmin")
    private Integer isSuperAdmin;
    private String mobilePhone;
    private String name;
    private String nickname;
    @JsonProperty("nickname_pinyin")
    private String nicknamePinyin;
    private String secretKey;
    private Long userid;
    private String username;
    private int validflag;

    public static TeslaUserInfoVO teslaUserDoToVo(UserDO teslaUser) {
        return TeslaUserInfoVO.builder().aliww(teslaUser.getAliww()).bucUserId(teslaUser.getBucId().toString())
            .createtime(teslaUser.getGmtCreate()).email(teslaUser.getEmail()).employeeId(teslaUser.getEmpId())
            .isPublicAccount(0).isSuperAdmin(0).mobilePhone(teslaUser.getPhone())
            .name(teslaUser.getLoginName()).nickname(teslaUser.getNickName()).nicknamePinyin(teslaUser.getNickName())
            .secretKey(teslaUser.getSecretKey()).userid(teslaUser.getId()).username(teslaUser.getUsername())
            .validflag(teslaUser.getIsLocked()).build();
    }
}
