package com.alibaba.sreworks.dataset.api.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.model.DataModelGroupFieldReq;

import java.util.List;

/**
 * 数据模型元信息接口
 */
public interface ModelMetaService extends BasicApi {

    /**
     * 根据模型ID查询模型配置信息
     * @return
     */
    List<JSONObject> getModelMetaById(Integer modelId);

    /**
     * 添加模型字段列
     * @param columnReq 列参数
     */
    void addModelColumn(DataModelGroupFieldReq columnReq) throws Exception ;

    /**
     * 删除模型字段列
     * @param modelId 模型ID
     */
    int deleteModelColumns(Integer modelId);

    /**
     * 删除模型字段列
     * @param modelId 模型ID
     * @param columnId 列ID
     */
    int deleteModelColumn(Integer modelId, Integer columnId);
}
