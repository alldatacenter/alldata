package com.datasophon.common.model;

import lombok.Data;

@Data
public class CheckResult {

    private Integer code;

    private String msg;

    public CheckResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
