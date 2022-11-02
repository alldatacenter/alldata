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
public class TabUpsertParam {

    private String id;

    private JSONArray elements;

    private String nodeTypePath;

    private String label;

    private String name;

    private JSONObject config;

}
