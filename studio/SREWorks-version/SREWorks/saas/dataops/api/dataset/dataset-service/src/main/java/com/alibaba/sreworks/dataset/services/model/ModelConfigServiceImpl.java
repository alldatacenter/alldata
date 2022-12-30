package com.alibaba.sreworks.dataset.services.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.DataInterfaceConfigService;
import com.alibaba.sreworks.dataset.api.model.ModelConfigService;
import com.alibaba.sreworks.dataset.common.exception.DomainNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ModelNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.domain.bo.DataModelGroupField;
import com.alibaba.sreworks.dataset.domain.bo.DataModelQueryField;
import com.alibaba.sreworks.dataset.domain.bo.DataModelValueField;
import com.alibaba.sreworks.dataset.domain.primary.*;
import com.alibaba.sreworks.dataset.domain.req.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据模型
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:19
 */
@Slf4j
@Service
@SuppressWarnings("Duplicates")
public class ModelConfigServiceImpl implements ModelConfigService {

    @Autowired
    DataSubjectMapper dataSubjectMapper;

    @Autowired
    DataDomainMapper dataDomainMapper;

    @Autowired
    DataModelConfigMapper dataModelConfigMapper;

    @Autowired
    DataInterfaceConfigService interfaceConfigService;

    @Override
    public JSONObject getModelConfigById(Integer modelId) {
        DataModelConfigWithBLOBs dataModelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        JSONObject result = convertToJSONObject(dataModelConfig);
        if (result.isEmpty()) {
            return result;
        }

        DataDomain dataDomain = dataDomainMapper.selectByPrimaryKey(dataModelConfig.getDomainId());
        if (dataDomain != null) {
            result.put("domainName", dataDomain.getName());
        }

        return result;
    }

    @Override
    public List<JSONObject> getModelConfigsByDomain(Integer domainId) {
        return getModelConfigs(null, domainId);
    }

    @Override
    public List<JSONObject> getModelConfigsNoDomainNameByDomain(Integer domainId) {
        DataModelConfigExample example = new DataModelConfigExample();
        example.createCriteria().andDomainIdEqualTo(domainId);
        List<DataModelConfigWithBLOBs> dataModelConfigs = dataModelConfigMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(dataModelConfigs);
    }

    @Override
    public List<JSONObject> getModelConfigsBySubject(Integer subjectId) {
        return getModelConfigs(subjectId, null);
    }

    @Override
    public List<JSONObject> getModelConfigs(Integer subjectId, Integer domainId) {
        // 查询主题信息
        DataSubjectExample subjectExample = new DataSubjectExample();
        if (subjectId != null) {
            subjectExample.createCriteria().andIdEqualTo(subjectId);
        } else {
            subjectExample.createCriteria().andIdIsNotNull();
        }
        List<DataSubject> dataSubjects = dataSubjectMapper.selectByExample(subjectExample);
        if (dataSubjects == null || dataSubjects.isEmpty()) {
            return convertToJSONObjects(dataSubjects);
        }
        Map<Integer, DataSubject> dataSubjectMaps = new HashMap<>();
        dataSubjects.forEach(dataSubject -> dataSubjectMaps.put(dataSubject.getId(), dataSubject));

        // 查询数据域信息
        DataDomainExample domainExample = new DataDomainExample();
        if (domainId == null) {
            domainExample.createCriteria().andSubjectIdIn(new ArrayList<>(dataSubjectMaps.keySet()));
        } else {
            domainExample.createCriteria().andIdEqualTo(domainId).andSubjectIdIn(new ArrayList<>(dataSubjectMaps.keySet()));
        }
        List<DataDomain> dataDomains = dataDomainMapper.selectByExample(domainExample);
        if (dataDomains == null || dataDomains.isEmpty()) {
            return convertToJSONObjects(dataDomains);
        }
        Map<Integer, DataDomain> dataDomainMaps = new HashMap<>();
        dataDomains.forEach(dataDomain -> dataDomainMaps.put(dataDomain.getId(), dataDomain));

        DataModelConfigExample configExample = new DataModelConfigExample();
        configExample.createCriteria().andDomainIdIn(new ArrayList<>(dataDomainMaps.keySet()));
        List<DataModelConfigWithBLOBs> dataModelConfigs = dataModelConfigMapper.selectByExampleWithBLOBs(configExample);
        List<JSONObject> results = convertToJSONObjects(dataModelConfigs);

        if (!results.isEmpty()) {
            results.forEach(result -> {
                DataDomain matchDomain = dataDomainMaps.get(result.getInteger("domainId"));
                result.put("domainName", matchDomain.getName());
                result.put("subjectId", matchDomain.getSubjectId());
                result.put("subjectName", dataSubjectMaps.get(matchDomain.getSubjectId()).getName());
            });
        }

        return results;
    }

    @Override
    public int addModelConfig(DataModelConfigCreateReq modelConfigReq) throws Exception {
        DataModelConfigWithBLOBs configWithBLOBs = buildBaseDataModelConfigWithBLOBs(modelConfigReq);
        return dataModelConfigMapper.insert(configWithBLOBs);
    }

    @Override
    public int updateModelConfig(DataModelConfigUpdateReq modelConfigReq) throws Exception {
        if (buildInModel(modelConfigReq.getId())) {
            throw new ParamException("内置模型不允许修改");
        }

        DataModelConfigWithBLOBs oldModelConfig = dataModelConfigMapper.selectByPrimaryKey(modelConfigReq.getId());
        if (oldModelConfig == null ) {
            throw new ModelNotExistException(String.format("数据模型不存在,请检查参数,模型ID:%s", modelConfigReq.getId()));
        }

        if (StringUtils.isEmpty(modelConfigReq.getSourceType())) {
            modelConfigReq.setSourceType(oldModelConfig.getSourceType());
        }
        if (modelConfigReq.getQueryFields() == null && StringUtils.isNotEmpty(oldModelConfig.getQueryFields())) {
            List<DataModelQueryFieldReq> queryFields = JSONObject.parseArray(oldModelConfig.getQueryFields(), DataModelQueryFieldReq.class);
            modelConfigReq.setQueryFields(queryFields.toArray(new DataModelQueryFieldReq[0]));
        }

        DataModelConfigWithBLOBs configWithBLOBs = buildBaseDataModelConfigWithBLOBs(modelConfigReq);
        configWithBLOBs.setId(modelConfigReq.getId());
        configWithBLOBs.setGmtCreate(null);
        return dataModelConfigMapper.updateByPrimaryKeySelective(configWithBLOBs);
    }

    @Override
    @Transactional
    public int deleteModelById(Integer modelId) throws Exception {
        // 需要判定是否内置模型
        if (buildInModel(modelId)) {
            throw new ParamException("内置模型不允许删除");
        }

        List<JSONObject> dataInterfaces = interfaceConfigService.getConfigsByModel(modelId);
        for(JSONObject dataInterface : dataInterfaces) {
            interfaceConfigService.deleteInterfaceById(dataInterface.getInteger("id"));
        }

        return dataModelConfigMapper.deleteByPrimaryKey(modelId);
    }

    public DataModelConfigWithBLOBs getModelConfigDtoById(Integer modelId) throws Exception {
        DataModelConfigWithBLOBs config = dataModelConfigMapper.selectByPrimaryKey(modelId);
        if (config == null) {
            throw new ModelNotExistException(String.format("数据模型不存在,模型ID:%s", modelId));
        }
        return config;
    }

    @Override
    public List<DataModelQueryField> getModelQueryFieldsById(Integer modelId) {
        DataModelConfigWithBLOBs dataModelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        if (dataModelConfig == null) {
            return null;
        }

        String queryFields = dataModelConfig.getQueryFields();
        return JSONArray.parseArray(queryFields, DataModelQueryField.class);
    }

    @Override
    public List<DataModelValueField> getModelValueFieldsById(Integer modelId) {
        DataModelConfigWithBLOBs dataModelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        if (dataModelConfig == null) {
            return null;
        }

        String valueFields = dataModelConfig.getValueFields();
        return JSONArray.parseArray(valueFields, DataModelValueField.class);
    }

    @Override
    public List<DataModelGroupField> getModelGroupFieldsById(Integer modelId) {
        DataModelConfigWithBLOBs dataModelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        if (dataModelConfig == null) {
            return null;
        }

        String groupFields = dataModelConfig.getGroupFields();
        return JSONArray.parseArray(groupFields, DataModelGroupField.class);
    }

    @Override
    public Long countModelsByDomain(Integer domainId) {
        DataModelConfigExample example = new DataModelConfigExample();
        example.createCriteria().andDomainIdEqualTo(domainId);
        return dataModelConfigMapper.countByExample(example);
    }

    @Override
    public Map<Integer, Long> countModelsByDomains(List<Integer> domainIds) {
        DataModelConfigExample example = new DataModelConfigExample();
        example.createCriteria().andDomainIdIn(domainIds);
        List<DataModelConfig> dataModelConfigs = dataModelConfigMapper.selectByExample(example);
        return parseCountModels(dataModelConfigs);
    }

    @Override
    public Map<Integer, Long> countModels() {
        DataModelConfigExample example = new DataModelConfigExample();
        List<DataModelConfig> dataModelConfigs = dataModelConfigMapper.selectByExample(example);
        return parseCountModels(dataModelConfigs);
    }

    @Override
    public boolean buildInModel(Integer modelId) {
        DataModelConfig modelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        return modelConfig != null && modelConfig.getBuildIn();
    }

    @Override
    public boolean existModel(Integer modelId) {
        return dataModelConfigMapper.selectByPrimaryKey(modelId) != null;
    }

    private Map<Integer, Long> parseCountModels(List<DataModelConfig> dataModelConfigs) {
        Map<Integer, Long> result = new HashMap<>();
        dataModelConfigs.forEach(dataDomain -> {
            Integer domainId = dataDomain.getDomainId();
            if (result.containsKey(domainId)) {
                result.put(domainId, result.get(domainId) + 1);
            } else {
                result.put(domainId, 1L);
            }
        });
        return result;
    }

    private DataModelConfigWithBLOBs buildBaseDataModelConfigWithBLOBs(DataModelConfigBaseReq modelConfigReq) throws Exception {
        if (modelConfigReq.getDomainId() != null) {
            if (dataDomainMapper.selectByPrimaryKey(modelConfigReq.getDomainId()) == null) {
                throw new DomainNotExistException(String.format("数据域不存在,请检查参数,数据域ID:%s", modelConfigReq.getDomainId()));
            }
        }

        DataModelConfigWithBLOBs configWithBLOBs = new DataModelConfigWithBLOBs();
        Date date = new Date();
        configWithBLOBs.setGmtCreate(date);
        configWithBLOBs.setGmtModified(date);
        configWithBLOBs.setName(modelConfigReq.getName());
        configWithBLOBs.setLabel(modelConfigReq.getLabel());
        configWithBLOBs.setBuildIn(false);
        configWithBLOBs.setDomainId(modelConfigReq.getDomainId());
        configWithBLOBs.setTeamId(modelConfigReq.getTeamId());
        configWithBLOBs.setSourceType(modelConfigReq.getSourceType());
        configWithBLOBs.setSourceId(modelConfigReq.getSourceId());
        configWithBLOBs.setSourceTable(modelConfigReq.getSourceTable());
        configWithBLOBs.setGranularity(modelConfigReq.getGranularity());
        configWithBLOBs.setQuery(modelConfigReq.getQuery());

        List<Object> queryFieldList = modelConfigReq.getQueryFields() != null ?
                Arrays.asList((Object[])modelConfigReq.getQueryFields()) : new ArrayList<>();
        if (!queryFieldList.isEmpty()) {
            configWithBLOBs.setQueryFields(new JSONArray(queryFieldList).toJSONString());
        }

        List<String> modelFields = new ArrayList<>();

        DataModelGroupFieldReq[] groupFieldReqs = modelConfigReq.getGroupFields();
        if (groupFieldReqs != null && groupFieldReqs.length > 0) {
            configWithBLOBs.setGroupFields(new JSONArray(Arrays.asList((Object[])groupFieldReqs)).toJSONString());
            for (DataModelGroupFieldReq req : groupFieldReqs) {
                modelFields.add(req.getField());
            }
        }

        DataModelValueFieldReq[] valueFieldReqs = modelConfigReq.getValueFields();
        if (valueFieldReqs != null && valueFieldReqs.length > 0) {
            configWithBLOBs.setValueFields(new JSONArray(Arrays.asList((Object[])valueFieldReqs)).toJSONString());
            for (DataModelValueFieldReq req : valueFieldReqs) {
                modelFields.add(req.getField());
            }
        }

        if (!modelFields.isEmpty()) {
            configWithBLOBs.setModelFields(StringUtils.join(modelFields, ","));
        }

        configWithBLOBs.setDescription(modelConfigReq.getDescription());

        return configWithBLOBs;
    }

    @Override
    public JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }

        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(obj));

        String queryFieldsStr = result.getString("queryFields");
        if (StringUtils.isNotEmpty(queryFieldsStr)) {
            result.put("queryFields", JSONArray.parseArray(queryFieldsStr));
        }

        String valueFieldsStr = result.getString("valueFields");
        if (StringUtils.isNotEmpty(valueFieldsStr)) {
            result.put("valueFields", JSONArray.parseArray(valueFieldsStr));
        }

        String groupFieldsStr = result.getString("groupFields");
        if (StringUtils.isNotEmpty(groupFieldsStr)) {
            result.put("groupFields", JSONArray.parseArray(groupFieldsStr));
        }

        return result;
    }

    @Override
    public List<JSONObject> convertToJSONObjects(List<? extends Object> objList) {
        if (objList == null || objList.isEmpty()) {
            return new ArrayList<>();
        }

        return Collections.synchronizedList(objList).parallelStream().map(this::convertToJSONObject).collect(Collectors.toList());
    }
}
