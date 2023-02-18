package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InstallState {
//    安装状态1:正在安装 2：安装成功 3：安装失败
    RUNNING(1,"正在安装"),
    SUCCESS(2,"安装成功"),
    FAILED(3,"安装失败");

    private int value;


    private String desc;

    InstallState(int value, String desc) {
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
