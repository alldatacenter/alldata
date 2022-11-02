package com.alibaba.tesla.productops.params;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.productops.DO.ProductopsNode;

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
public class NodeUpdateParam {

    private String nodeTypePath;

    private String version;

    private JSONObject config;

    public List<String> subtractionRoleList(ProductopsNode node) {

        JSONObject configJsonObject = JSONObject.parseObject(node.getConfig());
        JSONArray roleList = configJsonObject.getJSONArray("roleList");
        if (roleList != null) {
            roleList.removeAll(config.getJSONArray("roleList"));
            return roleList.toJavaList(String.class);
        }
        return new ArrayList<>();

    }

}
