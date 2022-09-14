package com.alibaba.tesla.authproxy.model.vo;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * @Date 2019-09-05 14:00
 * @Author cdx
 **/
@Data
public class UserInfoVO implements Serializable {

    private String empId;
    private String email;
    private String account;
    private String pinyin2;
    private String id;
    private String gid;
    private String nickNameCn;
    private String mobilePhone;
    private String name;
    private String deptDesc;
    private String cid;
    private String deptEnName;
    private String jobDesc;

    public String getAccount() {
        if (StringUtils.isEmpty(email)) {
            return "";
        } else {
            return email.substring(0, email.indexOf("@"));
        }
    }
}
