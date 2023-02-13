package com.alibaba.tesla.productops.params;

import com.alibaba.fastjson.JSONArray;
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
public class AppInitParam {

    private JSONObject config;

    private String templateName;

    private String appId;

    private JSONArray environments;

    private String version;

}
