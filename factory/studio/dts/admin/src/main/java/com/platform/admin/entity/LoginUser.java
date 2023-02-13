package com.platform.admin.entity;

import lombok.Data;

/**
 * @author AllDataDC on 2023/01/17
 */
@Data
public class LoginUser {

    private String username;
    private String password;
    private Integer rememberMe;

}
