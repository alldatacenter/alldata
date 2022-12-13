package com.platform.dts.entity;

import lombok.Data;

/**
 * Created by AllDataDC on 2022/11/17
 */
@Data
public class LoginUser {

    private String username;
    private String password;
    private Integer rememberMe;

}
