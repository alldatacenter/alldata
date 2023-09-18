package org.dromara.cloudeon.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色实例状态
 */
public enum ServiceRoleState {
    INIT_ROLE(0,"新增角色部署中"),
    STARTING_ROLE(1,"角色启动中"),
    ROLE_STARTED(2,"角色已启动"),
    ROLE_STOPPED(3,"角色已停止"),
    STOPPING_ROLE(4,"角色停止中");

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
