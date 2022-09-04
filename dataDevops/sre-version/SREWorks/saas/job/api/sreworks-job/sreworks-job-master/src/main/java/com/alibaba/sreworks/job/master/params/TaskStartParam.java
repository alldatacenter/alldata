package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TaskStartParam {

    private JSONObject varConf;

    public JSONObject varConf() {
        return varConf == null ? new JSONObject() : varConf;
    }

}
