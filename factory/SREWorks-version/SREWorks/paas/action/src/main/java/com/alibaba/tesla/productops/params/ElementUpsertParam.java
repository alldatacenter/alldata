package com.alibaba.tesla.productops.params;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementUpsertParam {

    private String elementId;

    private String name;

    private String version;

    private String type;

    private String appId;

    private JSONObject config;

    public String elementId() {
        return appId + ":" + type + ":" + name;
    }

}
