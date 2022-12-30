package com.alibaba.tesla.authproxy.api.lib;

import com.alibaba.tesla.authproxy.api.exception.AuthProxyParamException;

public enum AuthProxyResourceEnum {

    // 获取用户信息
    GET_USER_INFO,

    // 内部获取用户信息
    GET_USER_DETAIL,

    // 旧版获取用户信息接口
    OLD_GET_USER_INFO;

    public String toURI(Object... params) throws AuthProxyParamException {
        switch (this) {
            case GET_USER_INFO:
                return "/auth/user/info";
            case GET_USER_DETAIL:
                return "/auth/user/detail";
            case OLD_GET_USER_INFO:
                return "/auth/user/loginUser";
            default:
                throw new AuthProxyParamException(String.format("Unknown authproxy resource %s", this.toString()));
        }
    }

}
