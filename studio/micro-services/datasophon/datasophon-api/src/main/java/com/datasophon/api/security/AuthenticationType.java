package com.datasophon.api.security;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * authentication type
 */
public enum AuthenticationType {

    PASSWORD(0, "verify via user name and password"),
    ;

    AuthenticationType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @EnumValue
    private final int code;
    private final String desc;
}
