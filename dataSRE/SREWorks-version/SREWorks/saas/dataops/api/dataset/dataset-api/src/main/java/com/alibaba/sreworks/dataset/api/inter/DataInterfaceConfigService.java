package com.alibaba.sreworks.dataset.api.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceConfigUpdateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamUpdateReq;

import java.util.List;

public interface DataInterfaceConfigService extends BasicApi {

    /**
     * 查询接口配置
     * @param interfaceId
     * @return
     */
    JSONObject getConfigById(Integer interfaceId);

    /**
     * 根据接口标识查询接口配置
     * @param label
     * @return
     */
    JSONObject getConfigByLabel(String label) throws Exception;

    /**
     * 根据模型标识查询接口配置
     * @param modelId
     * @return
     */
    List<JSONObject> getConfigsByModel(Integer modelId);

    /**
     * 根据数据域标识查询接口配置
     * @param domainId
     * @return
     */
    List<JSONObject> getConfigsByDomain(Integer domainId);

    /**
     * 根据数据域标识查询接口配置
     * @param domainId
     * @return
     */
    List<JSONObject> getConfigs(Integer subjectId, Integer domainId, Integer modelId);

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
    int deleteInterfaceById(Integer interfaceId) throws Exception;

    /**
     * 新建数据接口
     * @param interfaceConfigReq
     * @param paramReqList
     * @return
     * @throws Exception
     */
    int addInterfaceConfigWithParams(DataInterfaceConfigCreateReq interfaceConfigReq, List<DataInterfaceParamCreateReq> paramReqList) throws Exception;

    /**
     * 更新数据接口
     * @param interfaceConfigReq
     * @param paramReqList
     * @return
     * @throws Exception
     */
    int updateInterfaceConfigWithParams(DataInterfaceConfigUpdateReq interfaceConfigReq, List<DataInterfaceParamUpdateReq> paramReqList) throws Exception;
}
