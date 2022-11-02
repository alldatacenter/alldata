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
public class NodeInsertParam {

    private String category;

    private String parentNodeTypePath;

    private String serviceType;

    private String version;

    private JSONObject config;

}
