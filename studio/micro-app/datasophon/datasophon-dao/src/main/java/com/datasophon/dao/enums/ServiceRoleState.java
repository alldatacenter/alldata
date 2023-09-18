package com.datasophon.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceRoleState {
    RUNNING(1, "正在运行"),
    STOP(2, "停止"),
    EXISTS_ALARM(3, "存在告警"),
    DECOMMISSIONING(4,"退役中"),
    DECOMMISSIONED(5,"已退役")
    ;

    @EnumValue
    private int value;

    private String desc;

    ServiceRoleState(int value, String desc) {
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
