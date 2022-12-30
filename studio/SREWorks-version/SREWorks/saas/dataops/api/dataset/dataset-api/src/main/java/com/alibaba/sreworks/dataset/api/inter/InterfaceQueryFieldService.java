package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceQueryFieldCreateReq;

import java.util.List;

public interface InterfaceQueryFieldService extends BasicApi {

    /**
     * 根据接口ID查询字段列表
     * @param interfaceId
     * @return
     */
    List<JSONObject> getFieldsByInterfaceId(Integer interfaceId);

    /**
     * 根据接口ID删除字段
     * @param interfaceId
     * @return
     */
    int deleteFieldsByInterfaceId(Integer interfaceId) throws Exception;

    /**
     * 新增查询字段
     * @param req
     * @return
     * @throws Exception
     */
    int addField(InterfaceQueryFieldCreateReq req) throws Exception;
}
