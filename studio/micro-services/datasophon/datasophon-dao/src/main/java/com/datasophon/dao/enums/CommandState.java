package com.datasophon.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CommandState {
    //命令状态 1：正在运行2：成功3：失败
    WAIT(0,"待运行"),
    RUNNING(1,"正在运行"),
    SUCCESS(2,"成功"),
    FAILED(3,"失败"),
    CANCEL(4,"取消");

    @EnumValue
    private int value;

    private String desc;

    CommandState(int value, String desc) {
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
