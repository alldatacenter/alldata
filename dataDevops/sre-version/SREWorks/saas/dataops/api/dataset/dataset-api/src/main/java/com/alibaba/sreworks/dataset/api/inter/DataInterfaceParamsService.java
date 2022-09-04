package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamUpdateReq;

import java.util.List;

public interface DataInterfaceParamsService extends BasicApi {
    /**
     * 查询接口参数
     * @param interfaceId
     * @return
     */
    List<JSONObject> getParams(Integer interfaceId);

    /**
     * 新增接口参数
     * @param paramReq
     * @throws Exception
     */
    int addInterfaceParam(DataInterfaceParamCreateReq paramReq) throws Exception;

    /**
     * 新增接口参数
     * @param paramReq
     * @throws Exception
     */
    int updateInterfaceParam(DataInterfaceParamUpdateReq paramReq) throws Exception;

    /**
     * 删除接口参数
     * @param interfaceId
     * @return
     */
    int deleteInterfaceParams(Integer interfaceId) throws Exception;

    /**
     * 删除接口参数
     * @param interfaceId
     * @param paramId
     * @return
     */
    int deleteInterfaceParam(Integer interfaceId, Long paramId) throws Exception;
}
