package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceRoleType {
    MASTER(1,"master"),
    WORKER(2,"worker"),
    CLIENT(3,"client"),
    SLAVE(4,"slave");

    private Integer code;
    private String name;

    ServiceRoleType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
