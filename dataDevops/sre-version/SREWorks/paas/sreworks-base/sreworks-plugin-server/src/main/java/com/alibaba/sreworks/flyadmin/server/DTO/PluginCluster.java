package com.alibaba.sreworks.flyadmin.server.DTO;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginCluster {

    private String name;

    private String alias;

    private String status;

    private Boolean isNormal;

    private JSONObject detail;
}
