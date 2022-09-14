package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceGroupFieldCreateReq;

import java.util.List;

public interface InterfaceSortFieldService extends BasicApi {

    List<JSONObject> getFieldsByInterfaceId(Integer interfaceId);

    /**
     * 根据接口ID删除字段
     * @param interfaceId
     * @return
     */
    int deleteFieldsByInterfaceId(Integer interfaceId) throws Exception;

    /**
     * 新增排序字段
     * @param req
     * @return
     * @throws Exception
     */
    int addField(InterfaceGroupFieldCreateReq req) throws Exception;
}
