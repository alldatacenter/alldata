package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public interface InterfaceService {

    /**
     * 查询接口
     * @param name 接口名称
     * @param params 接口url参数
     * @return 查询结果
     */
    Map<String, Object> get(String name, Map<String, Object> params) throws Exception;

    /**
     * 查询接口
     * @param name 接口名称
     * @param params 接口url参数
     * @param body 接口body参数
     * @return 查询结果
     */
    Map<String, Object> get(String name, Map<String, Object> params, JSONObject body) throws Exception;
}
