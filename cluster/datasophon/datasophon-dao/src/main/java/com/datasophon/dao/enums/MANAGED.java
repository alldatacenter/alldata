package com.datasophon.dao.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum MANAGED {
    YES(1,true),
    NO(2,false);


    @EnumValue
    private int value;


    private boolean desc;

    MANAGED(int value, boolean desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isDesc() {
        return desc;
    }

    public void setDesc(boolean desc) {
        this.desc = desc;
    }

}
