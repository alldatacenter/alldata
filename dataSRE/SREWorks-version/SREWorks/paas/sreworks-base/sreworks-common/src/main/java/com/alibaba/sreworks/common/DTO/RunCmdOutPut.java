package com.alibaba.sreworks.common.DTO;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RunCmdOutPut {

    private String cmd;

    private Integer timeout;

    private Boolean isNormal;

    private Integer code;

    private String stdout;

    private String stderr;

    public String toString() {
        return JSONObject.toJSONString(this, true);
    }

}
