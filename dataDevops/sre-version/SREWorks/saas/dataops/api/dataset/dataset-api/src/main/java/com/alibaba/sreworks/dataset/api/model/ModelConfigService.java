package com.alibaba.sreworks.dataset.api.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.bo.DataModelGroupField;
import com.alibaba.sreworks.dataset.domain.bo.DataModelQueryField;
import com.alibaba.sreworks.dataset.domain.bo.DataModelValueField;
import com.alibaba.sreworks.dataset.domain.req.model.DataModelConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.model.DataModelConfigUpdateReq;

import java.util.List;
import java.util.Map;

/**
 * 数据模型配置信息接口
 */
public interface ModelConfigService extends BasicApi {
    /**
     * 根据模型ID查询模型配置信息
     * @param modelId 数据模型ID
     * @return
     */
    JSONObject getModelConfigById(Integer modelId);

    /**
     * 查询数据域下的模型信息
     * @param domainId 数据域ID
     * @return
     */
    List<JSONObject> getModelConfigsByDomain(Integer domainId);

    /**
     * 查询数据域下的模型信息(不带domainName)
     * @param domainId 数据域ID
     * @return
     */
    List<JSONObject> getModelConfigsNoDomainNameByDomain(Integer domainId);

    /**
     *  查询数据主题下的模型信息
     * @param subjectId
     * @return
     */
    List<JSONObject> getModelConfigsBySubject(Integer subjectId);

    /**
     * 查询数据模型配置(兼容全量查询)
     * @return
     */
    List<JSONObject> getModelConfigs(Integer subjectId, Integer domainId);

    /**
     * 新建模型
     * @param modelConfigReq
     * @return
     * @throws Exception
     */
    int addModelConfig(DataModelConfigCreateReq modelConfigReq) throws Exception;

    /**
     * 根据模型ID更新模型配置信息
     * @param modelConfigReq
     * @throws Exception
     */
    int updateModelConfig(DataModelConfigUpdateReq modelConfigReq) throws Exception;

    /**
     * 根据模型ID删除模型配置信息，同时会级联删除模型下的接口
     * @param modelId
     * @return
     * @throws Exception
     */
    int deleteModelById(Integer modelId) throws Exception;

    /**
     * 根据模型ID查询模型查询字段列表
     * @param modelId
     * @return
     */
    List<DataModelQueryField> getModelQueryFieldsById(Integer modelId);

    /**
     * 根据模型ID查询模型数值字段列表
     * @param modelId
     * @return
     */
    List<DataModelValueField> getModelValueFieldsById(Integer modelId);

    /**
     * 根据模型ID查询模型分组字段列表
     * @param modelId
     * @return
     */
    List<DataModelGroupField> getModelGroupFieldsById(Integer modelId);

    /**
     * 根据数据域统计模型数量
     * @param domainId
     * @return
     */
    Long countModelsByDomain(Integer domainId);

    /**
     * 根据数据域统计模型数量
     * @param
     * @return 数据域ID,模型数量
     */
    Map<Integer, Long> countModelsByDomains(List<Integer> domainIds);

    /**
     * 根据数据域统计模型数量
     * @param
     * @return 数据域ID,模型数量
     */
    Map<Integer, Long> countModels();

    /**
     * 内置数据模型
     * @param modelId
     * @return
     */
    boolean buildInModel(Integer modelId);

    /**
     * 数据模型存在
     * @param modelId
     * @return
     */
    boolean existModel(Integer modelId);
}
