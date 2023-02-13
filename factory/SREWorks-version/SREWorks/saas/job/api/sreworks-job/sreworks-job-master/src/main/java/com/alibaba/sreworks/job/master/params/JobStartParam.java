package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class JobStartParam {

    private JSONObject varConf;

    private JSONArray tags;

    private JSONArray traceIds;

    public List<String> tags() {
        return tags == null ? new ArrayList<>() : tags.toJavaList(String.class);
    }

    public List<String> traceIds() {
        return traceIds == null ? new ArrayList<>() : traceIds.toJavaList(String.class);
    }

    public JSONObject varConf() {
        return varConf == null ? new JSONObject() : varConf;
    }

}
