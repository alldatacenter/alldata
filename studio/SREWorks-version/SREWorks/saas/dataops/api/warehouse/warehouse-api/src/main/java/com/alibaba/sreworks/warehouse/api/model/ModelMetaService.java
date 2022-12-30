package com.alibaba.sreworks.warehouse.api.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.BasicApi;
import com.alibaba.sreworks.warehouse.domain.req.model.ModelCreateReq;
import com.alibaba.sreworks.warehouse.domain.req.model.ModelFieldCreateReq;
import com.alibaba.sreworks.warehouse.domain.req.model.ModelFieldUpdateReq;
import com.alibaba.sreworks.warehouse.domain.req.model.ModelUpdateReq;

import java.util.List;

/**
 * 数据模型元信息接口
 */
public interface ModelMetaService extends BasicApi {

    /**
     * 模型统计信息
     * @param id
     * @return
     */
    JSONObject statsModelById(Long id);

    /**
     * 根据模型ID查询模型信息
     * @return
     */
    JSONObject getModelById(Long id);

    /**
     * 根据模型名称查询模型信息
     * @return
     */
    JSONObject getModelByName(String name);

    /**
     * 根据数据域名称查询模型信息
     * @return
     */
    List<JSONObject> getModelsByDomain(String layer, Integer domainId);

    /**
     * 根据数据主题查询模型信息
     * @return
     */
    List<JSONObject> getModelsBySubject(String layer, String subject);

    /**
     * 根据数仓分层名称查询模型信息
     * @return
     */
    List<JSONObject> getModelsByLayer(String layer);

    /**
     * 查询模型列表
     * @return
     */
    List<JSONObject> getModels();

    /**
     * 查询模型列信息
     * @return
     */
    List<JSONObject> getFieldsByModelId(Long modelId) throws Exception;

    /**
     * 查询模型列信息
     * @return
     */
    List<JSONObject> getFieldsByModelName(String modelName) throws Exception;

    /**
     * 查询模型和模型列信息
     * @return
     */
    JSONObject getModelWithFieldsById(Long id);

    /**
     * 查询模型和模型列信息
     * @return
     */
    JSONObject getModelWithFieldsByName(String name);

    /**
     * 根据模型ID删除模型
     * @param id
     * @return
     */
    int deleteModelById(Long id) throws Exception;

    /**
     * 根据模型名称删除模型
     * @param name
     * @return
     */
    int deleteModelByName(String name) throws Exception;

    /**
     * 根据列ID删除列
     * @param modelId
     * @param fieldId
     * @return
     */
    int deleteFieldById(Long modelId, Long fieldId) throws Exception;

    /**
     * 根据列名删除列
     * @param modelId
     * @param fieldName
     * @return
     */
    int deleteFieldByName(Long modelId, String fieldName) throws Exception;

    /**
     * 创建模型
     * @param req
     * @return 模型ID
     */
    long createModel(ModelCreateReq req) throws Exception;

    /**
     * 创建模型(带列)
     * @param req
     * @return 模型ID
     */
    long createModelWithFields(ModelCreateReq req, List<ModelFieldCreateReq> fieldReqs) throws Exception;

    /**
     * 更新模型
     * @param req
     * @return 模型ID
     */
    long updateModel(ModelUpdateReq req) throws Exception;

    /**
     * 新增模型列
     */
    int addFieldByModelId(Long modelId, ModelFieldCreateReq req) throws Exception;

    /**
     * 更新模型列
     */
    int updateFieldByModelId(Long modelId, ModelFieldUpdateReq req) throws Exception;
}
