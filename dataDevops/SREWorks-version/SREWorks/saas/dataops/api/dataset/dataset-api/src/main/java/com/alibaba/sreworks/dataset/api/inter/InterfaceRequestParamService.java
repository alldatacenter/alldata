package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;

import java.util.List;

public interface InterfaceRequestParamService extends BasicApi {

    /**
     * 根据接口ID查询参数列表
     * @param interfaceId
     * @return
     */
    List<JSONObject> getParamsByInterfaceId(Integer interfaceId);

    /**
     * 根据接口ID删除参数
     * @param interfaceId
     * @return
     */
    int deleteParamsByInterfaceId(Integer interfaceId) throws Exception;
}
