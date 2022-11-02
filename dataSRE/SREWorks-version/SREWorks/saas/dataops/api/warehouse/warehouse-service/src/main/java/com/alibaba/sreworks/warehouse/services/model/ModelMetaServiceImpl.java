package com.alibaba.sreworks.warehouse.services.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.model.ModelMetaService;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.*;
import com.alibaba.sreworks.warehouse.domain.*;
import com.alibaba.sreworks.warehouse.domain.req.model.*;
import com.alibaba.sreworks.warehouse.operator.ESIndexOperator;
import com.alibaba.sreworks.warehouse.operator.ESSearchOperator;
import com.alibaba.sreworks.warehouse.services.DwCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型元管理Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/17 17:20
 */
@Slf4j
@Service
public class ModelMetaServiceImpl extends DwCommonService implements ModelMetaService {

    @Autowired
    SwDomainMapper domainMapper;

    @Autowired
    SwModelMapper modelMapper;

    @Autowired
    SwModelFieldMapper modelFieldMapper;

    @Autowired
    ESIndexOperator esIndexOperator;

    @Autowired
    ESSearchOperator esSearchOperator;

    @Override
    public JSONObject statsModelById(Long id) {
        SwModel model = modelMapper.selectByPrimaryKey(id);
        if (Objects.isNull(model)) {
            return convertToJSONObject(null);
        }

        JSONObject stats = new JSONObject();

        SwModelFieldExample fieldExample = new SwModelFieldExample();
        fieldExample.createCriteria().andModelIdEqualTo(id);
        long fieldCount = modelFieldMapper.countByExample(fieldExample);
        stats.put("fieldCount", fieldCount);

        JSONObject tableStats = statsTable(model.getTableName(), model.getTableAlias());
        stats.putAll(tableStats);

        return stats;
    }

    @Override
    public JSONObject getModelById(Long id) {
        SwModel model = modelMapper.selectByPrimaryKey(id);
        return richModelDomainInfo(model);
    }

    @Override
    public JSONObject getModelByName(String name) {
        SwModelExample example =  new SwModelExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(models)) {
            return convertToJSONObject(null);
        } else {
            return richModelDomainInfo(models.get(0));
        }
    }

    @Override
    public List<JSONObject> getModelsByDomain(String layer, Integer domainId) {
        SwModelExample example =  new SwModelExample();
        example.createCriteria().andLayerEqualTo(layer).andDomainIdEqualTo(domainId);
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(models)) {
            return convertToJSONObjects(null);
        } else {
            return richModelsDomainInfo(models);
        }
    }

    @Override
    public List<JSONObject> getModelsBySubject(String layer, String subject) {
        SwDomainExample domainExample = new SwDomainExample();
        domainExample.createCriteria().andSubjectEqualTo(subject);
        List<SwDomain> domains = domainMapper.selectByExample(domainExample);
        if (CollectionUtils.isEmpty(domains)) {
            return convertToJSONObjects(null);
        } else {
            List<Integer> domainIds = domains.stream().map(SwDomain::getId).collect(Collectors.toList());
            SwModelExample example =  new SwModelExample();
            example.createCriteria().andLayerEqualTo(layer).andDomainIdIn(domainIds);
            List<SwModel> models = modelMapper.selectByExampleWithBLOBs(example);
            if (CollectionUtils.isEmpty(models)) {
                return convertToJSONObjects(null);
            } else {
                return richModelsDomainInfo(models);
            }
        }
    }

    @Override
    public List<JSONObject> getModelsByLayer(String layer) {
        SwModelExample example =  new SwModelExample();
        example.createCriteria().andLayerEqualTo(layer);
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(models)) {
            return convertToJSONObjects(null);
        } else {
            return richModelsDomainInfo(models);
        }
    }

    @Override
    public List<JSONObject> getModels() {
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(new SwModelExample());
        return richModelsDomainInfo(models);
    }

    @Override
    public List<JSONObject> getFieldsByModelId(Long modelId) throws Exception {
        SwModel model = modelMapper.selectByPrimaryKey(modelId);
        if (Objects.isNull(model)) {
            throw new ModelNotExistException(String.format("模型[id:%s]不存在", modelId));
        }
        return convertToJSONObjects(getFieldsByModelIdNoCheck(modelId));
    }

    @Override
    public List<JSONObject> getFieldsByModelName(String modelName) throws Exception {
        SwModelExample modelExample =  new SwModelExample();
        modelExample.createCriteria().andNameEqualTo(modelName);
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(modelExample);
        if (CollectionUtils.isEmpty(models)) {
            throw new ModelNotExistException(String.format("模型[name:%s]不存在", modelName));
        }
        List<SwModelField> fields = getFieldsByModelIdNoCheck(models.get(0).getId());
        return convertToJSONObjects(fields);
    }

    @Override
    public JSONObject getModelWithFieldsById(Long id) {
        SwModel model = modelMapper.selectByPrimaryKey(id);
        if (Objects.isNull(model)) {
            return convertToJSONObject(null);
        }
        JSONObject result = richModelDomainInfo(model);
        result.put("fields", convertToJSONObjects(getFieldsByModelIdNoCheck(id)));

        return result;
    }

    @Override
    public JSONObject getModelWithFieldsByName(String name) {
        SwModelExample modelExample =  new SwModelExample();
        modelExample.createCriteria().andNameEqualTo(name);
        List<SwModel> models = modelMapper.selectByExampleWithBLOBs(modelExample);
        if (CollectionUtils.isEmpty(models)) {
            return convertToJSONObject(null);
        }
        SwModel swModel = models.get(0);
        JSONObject result = richModelDomainInfo(swModel);
        result.put("fields", convertToJSONObjects(getFieldsByModelIdNoCheck(swModel.getId())));

        return result;
    }

    private List<SwModelField> getFieldsByModelIdNoCheck(Long modelId) {
        SwModelFieldExample example = new SwModelFieldExample();
        example.createCriteria().andModelIdEqualTo(modelId);
        return modelFieldMapper.selectByExampleWithBLOBs(example);
    }

    private JSONObject richModelDomainInfo(SwModel model) {
        if (Objects.isNull(model)) {
            return convertToJSONObject(null);
        }

        JSONObject result = convertToJSONObject(model);
        if (Objects.nonNull(model.getDomainId())) {
            SwDomain swDomain = getDomainById(model.getDomainId());
            result.put("domainName", swDomain.getName());
            result.put("domainAbbreviation", swDomain.getAbbreviation());
        }
        return result;
    }

    private List<JSONObject> richModelsDomainInfo(List<SwModel> models) {
        if (CollectionUtils.isEmpty(models)) {
            return convertToJSONObjects(null);
        }

        List<Integer> domainIds = models.stream().map(SwModel::getDomainId).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(domainIds)) {
            return convertToJSONObjects(models);
        }
        
        List<SwDomain> swDomains = getDomainsByIds(domainIds);
        Map<Integer, SwDomain> swDomainMaps = new HashMap<>();
        swDomains.forEach(swDomain -> swDomainMaps.put(swDomain.getId(), swDomain));

        return models.stream().map(model -> {
            JSONObject result = convertToJSONObject(model);
            if (Objects.nonNull(model.getDomainId())) {
                result.put("domainName", swDomainMaps.get(model.getDomainId()).getName());
                result.put("domainAbbreviation", swDomainMaps.get(model.getDomainId()).getAbbreviation());
            }
            return result;
        }).collect(Collectors.toList());
    }

    private List<SwDomain> getDomainsByIds(List<Integer> ids) {
        SwDomainExample example = new SwDomainExample();
        example.createCriteria().andIdIn(ids);
        return domainMapper.selectByExampleWithBLOBs(example);
    }

    private SwDomain getDomainById(Integer id) {
        return domainMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional
    public int deleteModelById(Long id) throws Exception {
        SwModel model = modelMapper.selectByPrimaryKey(id);
        if (Objects.isNull(model)) {
            return 0;
        }
        if (hasBuildInModel(Collections.singletonList(model))) {
            throw new ParamException("内置模型不允许删除");
        }

        esIndexOperator.deleteIndexByAlias(model.getTableName(), model.getTableAlias());

        SwModelFieldExample example = new SwModelFieldExample();
        example.createCriteria().andModelIdEqualTo(id);
        modelFieldMapper.deleteByExample(example);

        return modelMapper.deleteByPrimaryKey(id);
    }

    @Override
    @Transactional
    public int deleteModelByName(String name) throws Exception {
        SwModelExample example =  new SwModelExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwModel> models = modelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(models)) {
            return 0;
        }

        SwModel model = models.get(0);
        if (hasBuildInModel(Collections.singletonList(model))) {
            throw new ParamException("内置模型不允许删除");
        }

        esIndexOperator.deleteIndexByAlias(model.getTableName(), model.getTableAlias());

        SwModelFieldExample fieldExample = new SwModelFieldExample();
        fieldExample.createCriteria().andModelIdEqualTo(model.getId());
        modelFieldMapper.deleteByExample(fieldExample);

        return modelMapper.deleteByExample(example);
    }

    @Override
    public int deleteFieldById(Long modelId, Long fieldId) throws Exception {
        // 仅仅删除字段定义, 不修改ES存储数据
        SwModelField modelField = modelFieldMapper.selectByPrimaryKey(fieldId);
        if (modelField != null) {
            if (hasBuildInModelFiled(Collections.singletonList(modelField))) {
                throw new ParamException("内置模型字段不允许删除");
            }
            return deleteFieldByName(modelId, modelField.getField());
        }
        return 0;
    }

    @Override
    public int deleteFieldByName(Long modelId, String fieldName) throws Exception {
        // 仅仅删除字段定义, 不修改ES存储数据
        SwModelFieldExample fieldExample = new SwModelFieldExample();
        fieldExample.createCriteria().andModelIdEqualTo(modelId).andFieldEqualTo(fieldName);

        List<SwModelField> modelFields = modelFieldMapper.selectByExample(fieldExample);
        if (hasBuildInModelFiled(modelFields)) {
            throw new ParamException("内置模型字段不允许删除");
        }
        return modelFieldMapper.deleteByExample(fieldExample);
    }

    @Override
    @Transactional
    public long createModel(ModelCreateReq req) throws Exception {
        if (!CollectionUtils.isEmpty(getModelByName(req.getName()))) {
            throw new ModelExistException(String.format("同名模型[%s]已存在", req.getName()));
        }

        SwModel swModel = buildSwModel(req);
        modelMapper.insert(swModel);

        ModelFieldCreateReq fieldCreateReq = new ModelFieldCreateReq();
        fieldCreateReq.setField(DwConstant.PARTITION_FIELD);
        fieldCreateReq.setDim(DwConstant.PARTITION_DIM);
        fieldCreateReq.setType(DwConstant.PARTITION_TYPE);
        fieldCreateReq.setBuildIn(DwConstant.PARTITION_BUILD_IN);
        fieldCreateReq.setAlias(DwConstant.PARTITION_ALIAS);
        fieldCreateReq.setNullable(DwConstant.PARTITION_NULLABLE);
        fieldCreateReq.setDescription(DwConstant.PARTITION_DESCRIPTION);

        SwModelField swModelField = buildSwModelField(swModel.getId(), fieldCreateReq);
        modelFieldMapper.insert(swModelField);

        createTableMeta(swModel.getTableName(), swModel.getTableAlias(), swModel.getLifecycle());

        return swModel.getId();
    }

    @Override
    @Transactional
    public long createModelWithFields(ModelCreateReq req, List<ModelFieldCreateReq> fieldReqs) throws Exception {
        boolean existId = fieldReqs.stream().anyMatch(fieldReq -> DwConstant.PRIMARY_FIELD.equals(fieldReq.getField()));
        if (!existId) {
            throw new ParamException("实体字段主键缺失, 需要定义id键字段");
        }

        long id = createModel(req);
        for (ModelFieldCreateReq fieldCreateReq : fieldReqs) {
            addFieldByModelId(id, fieldCreateReq);
        }

        return id;
    }

    @Override
    @Transactional
    public long updateModel(ModelUpdateReq req) throws Exception {
        SwModel oldModel = modelMapper.selectByPrimaryKey(req.getId());
        if (Objects.isNull(oldModel)) {
            throw new ModelNotExistException(String.format("模型[id:%s]不存在", req.getId()));
        }

        if (hasBuildInModel(Collections.singletonList(oldModel))) {
            throw new ParamException("内置模型不允许修改");
        }

        SwModel updateModel = new SwModel();
        updateModel.setId(req.getId());
        updateModel.setAlias(req.getAlias());
        updateModel.setGmtModified(new Date());
        updateModel.setLifecycle(req.getLifecycle());
        updateModel.setDescription(req.getDescription());

        modelMapper.updateByPrimaryKeySelective(updateModel);

        if (!oldModel.getLifecycle().equals(req.getLifecycle())) {
            updateTableLifecycle(oldModel.getTableName(), oldModel.getTableAlias(), req.getLifecycle());
        }

        return oldModel.getId();
    }

    @Override
    public int addFieldByModelId(Long modelId, ModelFieldCreateReq req) throws Exception {
        if (DwConstant.PARTITION_FIELD.equals(req.getField())) {
            throw new ModelFieldException(String.format("默认分区列冲突,%s", DwConstant.PARTITION_FIELD));
        }

        SwModel model = modelMapper.selectByPrimaryKey(modelId);
        if (Objects.isNull(model)) {
            throw new ModelNotExistException(String.format("模型[id:%s]不存在", modelId));
        }

        SwModelFieldExample fieldExample = new SwModelFieldExample();
        fieldExample.createCriteria().andModelIdEqualTo(modelId).andFieldEqualTo(req.getField());
        if(CollectionUtils.isEmpty(modelFieldMapper.selectByExample(fieldExample))) {
            SwModelField swModelField = buildSwModelField(modelId, req);
            return modelFieldMapper.insert(swModelField);
        } else {
            throw new ModelFieldExistException(String.format("模型[id:%s] 列[%s]已经存在", modelId, req.getField()));
        }
    }

    @Override
    public int updateFieldByModelId(Long modelId, ModelFieldUpdateReq req) throws Exception {
        SwModelField oldModelField = modelFieldMapper.selectByPrimaryKey(req.getId());
        if (oldModelField != null) {
            if (DwConstant.PARTITION_FIELD.equals(oldModelField.getField())) {
                throw new EntityFieldException(String.format("默认分区列冲突,%s", DwConstant.PARTITION_FIELD));
            }

            if (hasBuildInModelFiled(Collections.singletonList(oldModelField))) {
                throw new ParamException("内置模型字段不允许修改");
            }

            SwModelField updateModelField = buildSwModelField(modelId, req);
            updateModelField.setGmtCreate(null);
            updateModelField.setId(req.getId());
            return modelFieldMapper.updateByPrimaryKeySelective(updateModelField);
        }
        return 0;
    }

    private SwModel buildSwModel(ModelBaseReq req) throws Exception {
        SwModel swModel = new SwModel();
        Date now = new Date();
        swModel.setGmtCreate(now);
        swModel.setGmtModified(now);
        swModel.setName(req.getName());
        swModel.setAlias(req.getAlias());
        swModel.setLayer(req.getLayer());
        swModel.setBuildIn(req.getBuildIn());
        swModel.setPartitionFormat(req.getPartitionFormat());
        swModel.setDataMode(req.getDataMode());
        swModel.setStatPeriod(req.getStatPeriod());
        swModel.setDomainId(req.getDomainId());
        swModel.setLifecycle(req.getLifecycle());
        swModel.setTag(req.getTag());
        swModel.setDescription(req.getDescription());

        if(DwConstant.DW_ADS_LAYER.equals(req.getLayer())) {
            String tableAlias = req.getLayer() + "_" + req.getName().toLowerCase();
            swModel.setTableName("<" + tableAlias + "_" + DwConstant.INDEX_DATE_MATH.getString(req.getPartitionFormat()) + ">");
            swModel.setTableAlias(tableAlias);
        } else {
            SwDomain swDomain = domainMapper.selectByPrimaryKey(req.getDomainId());
            if (Objects.isNull(swDomain)) {
                throw new DomainNotExistException(String.format("数据域[id:%s]不存在", req.getDomainId()));
            }

            String tableAlias;
            if (DwConstant.DW_DWD_LAYER.equals(req.getLayer())) {
                tableAlias = req.getLayer() + "_" + swDomain.getAbbreviation() + "_" + req.getName().toLowerCase() + "_" + req.getDataMode();
            } else if (DwConstant.DW_DWS_LAYER.equals(req.getLayer())) {
                tableAlias = req.getLayer() + "_" + swDomain.getAbbreviation() + "_" + req.getName().toLowerCase() + "_" + req.getStatPeriod();
            } else {
                tableAlias = req.getLayer() + "_" + swDomain.getAbbreviation() + "_" + req.getName().toLowerCase();
            }
            swModel.setTableName("<" + tableAlias + "_" + DwConstant.INDEX_DATE_MATH.getString(req.getPartitionFormat()) + ">");
            swModel.setTableAlias(tableAlias);
        }

        return swModel;
    }

    private SwModelField buildSwModelField(Long modelId, ModelFieldBaseReq req) {
        SwModelField swModelField = new SwModelField();
        Date now = new Date();
        swModelField.setGmtCreate(now);
        swModelField.setGmtModified(now);
        swModelField.setField(req.getField());
        swModelField.setAlias(req.getAlias());
        swModelField.setModelId(modelId);
        swModelField.setDim(req.getDim());
        swModelField.setBuildIn(req.getBuildIn());
        swModelField.setType(req.getType().getType());
        swModelField.setNullable(req.getNullable());
        swModelField.setDescription(req.getDescription());

        return swModelField;
    }

    private void addTableField() {
        // TODO 按照数据类型做mapping映射
    }

    private boolean hasBuildInModel(List<SwModel> models) {
        Optional<SwModel> optional = models.stream().filter(SwModel::getBuildIn).findAny();
        return optional.isPresent();
    }

    private boolean hasBuildInModelFiled(List<SwModelField> modelFields) {
        Optional<SwModelField> optional = modelFields.stream().filter(SwModelField::getBuildIn).findAny();
        return optional.isPresent();
    }
}
