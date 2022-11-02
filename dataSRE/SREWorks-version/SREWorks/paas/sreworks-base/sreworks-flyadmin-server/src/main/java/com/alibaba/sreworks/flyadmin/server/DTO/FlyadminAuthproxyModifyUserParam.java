package com.alibaba.sreworks.flyadmin.server.DTO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class FlyadminAuthproxyModifyUserParam {

    String nickName;

    String loginName;

    String password;

    String email;

    String phone;

    String avatar;

}
