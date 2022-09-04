package com.alibaba.tesla.gateway.server.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class UserInfo {

    /**
     * 用户名称
     */
    @NotEmpty(message = "authUser can't be empty")
    private String authUser;

    /**
     * 用户工号
     */
    private String empId;
}
