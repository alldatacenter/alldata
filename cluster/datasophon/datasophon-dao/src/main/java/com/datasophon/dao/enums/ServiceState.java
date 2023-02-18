package com.datasophon.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceState {
    WAIT_INSTALL(1,"待安装"),
    RUNNING(2, "正常"),
    EXISTS_ALARM(3, "存在告警"),
    EXISTS_EXCEPTION(4, "存在异常"),
    ;

    @EnumValue
    private int value;

    private String desc;

    ServiceState(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    @Override
    public String toString() {
        return this.desc;
    }
}
