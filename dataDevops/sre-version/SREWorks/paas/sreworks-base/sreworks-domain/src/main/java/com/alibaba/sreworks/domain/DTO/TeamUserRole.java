package com.alibaba.sreworks.domain.DTO;

public enum TeamUserRole {

    ADMIN("管理员"),

    GUEST("成员");

    private final String cn;

    TeamUserRole(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return this.cn;
    }

}
