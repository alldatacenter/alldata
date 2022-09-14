package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigUpdateReq;

import java.util.List;

public interface InterfaceConfigService extends BasicApi {
    /**
     * 查询接口配置
     * @param interfaceId
     * @return
     */
    JSONObject getConfigById(Integer interfaceId);

    /**
     * 查询接口配置
     * @param name
     * @return
     */
    JSONObject getConfigByName(String name);

    /**
     * 查询所有接口
     * @return
     */
    List<JSONObject> getConfigs();

    /**
     * 数据接口存在
     * @param interfaceId
     * @return
     */
    boolean existInterface(Integer interfaceId);

    /**
     * 内置接口
     * @param interfaceId
     * @return
     * @throws Exception
     */
    boolean buildInInterface(Integer interfaceId);

    /**
     * 删除接口
     * @param interfaceId
     * @return
     */
    int deleteConfigById(Integer interfaceId) throws Exception;

    /**
     * 新建数据接口
     * @param req
     * @return
     * @throws Exception
     */
    int addConfig(InterfaceConfigCreateReq req) throws Exception;

    /**
     * 更新数据接口
     * @param req
     * @return
     * @throws Exception
     */
    int updateConfig(InterfaceConfigUpdateReq req) throws Exception;
}
