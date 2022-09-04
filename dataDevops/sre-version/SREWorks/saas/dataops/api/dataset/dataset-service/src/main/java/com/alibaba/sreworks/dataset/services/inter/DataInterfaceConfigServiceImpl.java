package com.alibaba.sreworks.dataset.services.inter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.DataInterfaceConfigService;
import com.alibaba.sreworks.dataset.api.inter.DataInterfaceParamsService;
import com.alibaba.sreworks.dataset.common.exception.InterfaceConfigException;
import com.alibaba.sreworks.dataset.common.exception.InterfaceNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ModelNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.domain.bo.DataModelQueryField;
import com.alibaba.sreworks.dataset.domain.primary.*;
import com.alibaba.sreworks.dataset.domain.req.inter.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据接口配置Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 22:06
 */

@Slf4j
@Service
@SuppressWarnings("Duplicates")
public class DataInterfaceConfigServiceImpl implements DataInterfaceConfigService {

    @Autowired
    DataInterfaceConfigMapper interfaceConfigMapper;

    @Autowired
    DataInterfaceParamsService interfaceParamsService;

    @Autowired
    DataModelConfigMapper dataModelConfigMapper;

    @Autowired
    DataDomainMapper dataDomainMapper;

    @Autowired
    DataSubjectMapper dataSubjectMapper;

    @Override
    public JSONObject getConfigById(Integer interfaceId) {
        DataInterfaceConfig config = interfaceConfigMapper.selectByPrimaryKey(interfaceId);
        JSONObject result = convertToJSONObject(config);
        if (result.isEmpty()) {
            return result;
        }

        DataModelConfigWithBLOBs modelConfig = dataModelConfigMapper.selectByPrimaryKey(config.getModelId());
        if (modelConfig != null) {
            result.put("modelLabel", modelConfig.getLabel());
            result.put("modelName", modelConfig.getName());
        }

        List<JSONObject> params = interfaceParamsService.getParams(interfaceId);
        result.put("params", params);

        return result;
    }

    @Override
    public JSONObject getConfigByLabel(String label) throws Exception {
        DataInterfaceConfigExample example = new DataInterfaceConfigExample();
        example.createCriteria().andLabelEqualTo(label);
        List<DataInterfaceConfig> configs = interfaceConfigMapper.selectByExampleWithBLOBs(example);
        if (configs.isEmpty()) {
            throw new InterfaceNotExistException(String.format("数据接口不存在, 接口标识:%s", label));
        } else {
            DataInterfaceConfig config = configs.get(0);
            JSONObject result = convertToJSONObject(config);
            if (result.isEmpty()) {
                return result;
            }

            DataModelConfigWithBLOBs modelConfig = dataModelConfigMapper.selectByPrimaryKey(config.getModelId());
            if (modelConfig != null) {
                result.put("modelLabel", modelConfig.getLabel());
                result.put("modelName", modelConfig.getName());
            }

            List<JSONObject> params = interfaceParamsService.getParams(config.getId());
            result.put("params", params);

            return result;
        }
    }

    public DataInterfaceConfig getConfigDtoByLabel(String label) throws Exception {
        DataInterfaceConfigExample example = new DataInterfaceConfigExample();
        example.createCriteria().andLabelEqualTo(label);
        List<DataInterfaceConfig> configs = interfaceConfigMapper.selectByExampleWithBLOBs(example);
        if (configs.isEmpty()) {
            throw new InterfaceNotExistException(String.format("数据接口不存在, 接口标识:%s", label));
        } else {
            return configs.get(0);
        }
    }

    @Override
    public List<JSONObject> getConfigsByModel(Integer modelId) {
        return getConfigs(null, null, modelId);
    }

    @Override
    public List<JSONObject> getConfigsByDomain(Integer domainId) {
        return getConfigs(null, domainId, null);
    }

    @Override
    public List<JSONObject> getConfigs(Integer subjectId, Integer domainId, Integer modelId) {
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

        // 查询数据模型信息
        DataModelConfigExample configExample = new DataModelConfigExample();
        if (modelId == null) {
            configExample.createCriteria().andDomainIdIn(new ArrayList<>(dataDomainMaps.keySet()));
        } else {
            configExample.createCriteria().andIdEqualTo(modelId).andDomainIdIn(new ArrayList<>(dataDomainMaps.keySet()));
        }
        List<DataModelConfig> dataModelConfigs = dataModelConfigMapper.selectByExample(configExample);
        if (dataModelConfigs == null || dataModelConfigs.isEmpty()) {
            return convertToJSONObjects(dataModelConfigs);
        }
        Map<Integer, DataModelConfig> dataModelConfigMaps = new HashMap<>();
        dataModelConfigs.forEach(dataModelConfig -> dataModelConfigMaps.put(dataModelConfig.getId(), dataModelConfig));

        DataInterfaceConfigExample example = new DataInterfaceConfigExample();
        example.createCriteria().andModelIdIn(new ArrayList<>(dataModelConfigMaps.keySet()));
        List<DataInterfaceConfig> configs = interfaceConfigMapper.selectByExampleWithBLOBs(example);
        List<JSONObject> results = convertToJSONObjects(configs);
        if (!results.isEmpty()) {
            results.forEach(result -> {
                DataModelConfig matchModelConfig = dataModelConfigMaps.get(result.getInteger("modelId"));
                DataDomain matchDomain = dataDomainMaps.get(matchModelConfig.getDomainId());
                result.put("modelName", matchModelConfig.getName());
                result.put("modelLabel", matchModelConfig.getLabel());
                result.put("domainId", matchDomain.getId());
                result.put("domainName", matchDomain.getName());
                result.put("subjectId", matchDomain.getSubjectId());
                result.put("subjectName", dataSubjectMaps.get(matchDomain.getSubjectId()).getName());
            });
        }

        return results;
    }

    @Override
    public boolean existInterface(Integer interfaceId) {
        return interfaceConfigMapper.selectByPrimaryKey(interfaceId) != null;
    }

    @Override
    @Transactional
    public int deleteInterfaceById(Integer interfaceId) throws Exception {
        if (buildInInterface(interfaceId)) {
            throw new ParamException("内置模型不允许修改");
        }

        interfaceParamsService.deleteInterfaceParams(interfaceId);
        return interfaceConfigMapper.deleteByPrimaryKey(interfaceId);
    }

    @Override
    public int addInterfaceConfigWithParams(DataInterfaceConfigCreateReq interfaceConfigReq, List<DataInterfaceParamCreateReq> paramReqList) throws Exception {
        DataModelConfigWithBLOBs modelConfig = dataModelConfigMapper.selectByPrimaryKey(interfaceConfigReq.getModelId());
        validRespAndParamFields(modelConfig, interfaceConfigReq, paramReqList);

        DataInterfaceConfig interfaceConfig = buildBaseDataModelConfigWithBLOBs(interfaceConfigReq);
        int result = interfaceConfigMapper.insert(interfaceConfig);

        Integer interfaceId = interfaceConfig.getId();
        log.info(String.format("新增的数据接口ID:%s", interfaceId));

        for (DataInterfaceParamCreateReq paramReq : paramReqList) {
            paramReq.setInterfaceId(interfaceId);
            interfaceParamsService.addInterfaceParam(paramReq);
        }

        return result;
    }

    @Override
    public int updateInterfaceConfigWithParams(DataInterfaceConfigUpdateReq interfaceConfigReq, List<DataInterfaceParamUpdateReq> paramReqList) throws Exception {
        if (!existInterface(interfaceConfigReq.getId())) {
            throw new InterfaceNotExistException("数据接口不存在");
        }

        if (buildInInterface(interfaceConfigReq.getId())) {
            throw new ParamException("内置模型不允许修改");
        }

        DataModelConfigWithBLOBs modelConfig = dataModelConfigMapper.selectByPrimaryKey(interfaceConfigReq.getModelId());
        validRespAndParamFields(modelConfig, interfaceConfigReq, paramReqList);

        Integer interfaceId = interfaceConfigReq.getId();
        DataInterfaceConfig interfaceConfig = buildBaseDataModelConfigWithBLOBs(interfaceConfigReq);
        interfaceConfig.setId(interfaceId);
        interfaceConfig.setGmtCreate(null);

        interfaceConfigMapper.updateByPrimaryKeySelective(interfaceConfig);

        // 删除旧的数据列
        List<Long>  interfaceOldParamIds = interfaceParamsService.getParams(interfaceId).stream().map(interfaceOldParam -> interfaceOldParam.getLong("id")).collect(Collectors.toList());
        for (Long id : interfaceOldParamIds) {
            interfaceParamsService.deleteInterfaceParam(interfaceId, id);
        }

        // 重建数据列
        for (DataInterfaceParamUpdateReq paramReq : paramReqList) {
            DataInterfaceParamCreateReq createReq = new DataInterfaceParamCreateReq();
            createReq.setLabel(paramReq.getLabel());
            createReq.setName(paramReq.getName());
            createReq.setInterfaceId(interfaceId);
            createReq.setRequired(paramReq.getRequired());
            createReq.setType(paramReq.getType());
            createReq.setDefaultValue(paramReq.getDefaultValue());
            interfaceParamsService.addInterfaceParam(createReq);
        }

        return 1;
    }

    @Override
    public boolean buildInInterface(Integer interfaceId) {
        DataInterfaceConfig interfaceConfig = interfaceConfigMapper.selectByPrimaryKey(interfaceId);
        return interfaceConfig != null && interfaceConfig.getBuildIn();
    }

    private void validRespAndParamFields(DataModelConfigWithBLOBs modelConfig, DataInterfaceConfigBaseReq interfaceConfigReq, List<? extends DataInterfaceParamBaseReq> paramReqList) throws Exception {
        List<String> modelFields = Arrays.asList(modelConfig.getModelFields().split(","));
        List<String> respFields = Arrays.asList(interfaceConfigReq.getResponseFields().split(","));
        if (!modelFields.containsAll(respFields)) {
            throw new InterfaceConfigException("数据接口返回字段必须是数据模型字段子集");
        }

        List<DataModelQueryField> modelQueryFields = JSONArray.parseArray(modelConfig.getQueryFields(), DataModelQueryField.class);
        Map<String, DataModelQueryField> modelQueryFieldsMap = modelQueryFields.stream().collect(
                Collectors.toMap(DataModelQueryField::getField, modelQueryField -> modelQueryField));

        for (DataInterfaceParamBaseReq paramReq : paramReqList) {
            DataModelQueryField modelQueryField = modelQueryFieldsMap.getOrDefault(paramReq.getLabel(), null);
            if (modelQueryField == null || !modelQueryField.getType().equals(paramReq.getType())) {
                throw new InterfaceConfigException("数据接口参数字段必须是数据模型查询字段的子集");
            }
        }
    }

    private DataInterfaceConfig buildBaseDataModelConfigWithBLOBs(DataInterfaceConfigBaseReq req) throws Exception {
        if (req.getModelId() != null) {
            if (dataModelConfigMapper.selectByPrimaryKey(req.getModelId()) == null) {
                throw new ModelNotExistException(String.format("数据模型不存在,请检查参数,模型ID:%s", req.getModelId()));
            }
        }

        DataInterfaceConfig interfaceConfig = new DataInterfaceConfig();
        Date date = new Date();
        interfaceConfig.setGmtCreate(date);
        interfaceConfig.setGmtModified(date);
        interfaceConfig.setLabel(req.getLabel());
        interfaceConfig.setName(req.getName());
        interfaceConfig.setBuildIn(false);
        interfaceConfig.setModelId(req.getModelId());
        interfaceConfig.setTeamId(req.getTeamId());
        interfaceConfig.setCreator(req.getCreator());
        interfaceConfig.setOwners(req.getOwners());
        interfaceConfig.setRequestMethod(req.getRequestMethod());
        interfaceConfig.setContentType(req.getContentType());
        interfaceConfig.setResponseFields(req.getResponseFields());
        interfaceConfig.setPaging(req.getPaging());

        DataInterfaceSortFieldReq[] sortFields = req.getSortFields();
        if (sortFields != null && sortFields.length > 0) {
            interfaceConfig.setSortFields(new JSONArray(Arrays.asList((Object[])sortFields)).toJSONString());
        }

        return interfaceConfig;
    }

    @Override
    public JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }

        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(obj));

        String sortFieldsStr = result.getString("sortFields");
        if (StringUtils.isNotEmpty(sortFieldsStr)) {
            result.put("sortFields", JSONArray.parseArray(sortFieldsStr));
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
