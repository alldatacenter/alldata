package com.alibaba.sreworks.warehouse.services.entity;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.entity.EntityMetaService;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.*;
import com.alibaba.sreworks.warehouse.domain.*;
import com.alibaba.sreworks.warehouse.domain.req.entity.*;
import com.alibaba.sreworks.warehouse.operator.ESIndexOperator;
import com.alibaba.sreworks.warehouse.services.DwCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 实体元管理Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/17 17:20
 */
@Slf4j
@Service
public class EntityMetaServiceImpl extends DwCommonService implements EntityMetaService {

    @Autowired
    SwDomainMapper domainMapper;

    @Autowired
    SwEntityMapper entityMapper;

    @Autowired
    SwEntityFieldMapper entityFieldMapper;

    @Autowired
    ESIndexOperator esIndexOperator;

    @Override
    public JSONObject statsEntityById(Long id) {
        SwEntity entity = entityMapper.selectByPrimaryKey(id);
        if (entity == null) {
            return convertToJSONObject(null);
        }

        JSONObject stats = new JSONObject();

        SwEntityFieldExample fieldExample = new SwEntityFieldExample();
        fieldExample.createCriteria().andEntityIdEqualTo(id);
        long fieldCount = entityFieldMapper.countByExample(fieldExample);
        stats.put("fieldCount", fieldCount);

        JSONObject tableStats = statsTable(entity.getTableName(), entity.getTableAlias());
        stats.putAll(tableStats);

        return stats;
    }

    @Override
    public JSONObject getEntityById(Long id) {
        SwEntity entity = entityMapper.selectByPrimaryKey(id);
        return convertToJSONObject(entity);
    }

    @Override
    public JSONObject getEntityByName(String name) {
        SwEntityExample example =  new SwEntityExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwEntity> entities = entityMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(entities)) {
            return convertToJSONObject(null);
        } else {
            return convertToJSONObject(entities.get(0));
        }
    }

    @Override
    public List<JSONObject> getEntitiesByLayer(String layer) {
        SwEntityExample example =  new SwEntityExample();
        example.createCriteria().andLayerEqualTo(layer);
        List<SwEntity> entities = entityMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(entities);
    }

    @Override
    public List<JSONObject> getEntities() {
        List<SwEntity> entities = entityMapper.selectByExampleWithBLOBs(new SwEntityExample());
        return convertToJSONObjects(entities);
    }

    @Override
    public List<JSONObject> getFieldsByEntityId(Long entityId) throws Exception {
        SwEntity entity = entityMapper.selectByPrimaryKey(entityId);
        if (entity == null) {
            throw new EntityNotExistException(String.format("实体[id:%s]不存在", entityId));
        }
        return convertToJSONObjects(getFieldsByEntityIdNoCheck(entityId));
    }

    @Override
    public List<JSONObject> getFieldsByEntityName(String entityName) throws Exception {
        SwEntityExample entityExample =  new SwEntityExample();
        entityExample.createCriteria().andNameEqualTo(entityName);
        List<SwEntity> entities = entityMapper.selectByExample(entityExample);
        if (CollectionUtils.isEmpty(entities)) {
            throw new EntityNotExistException(String.format("实体[name:%s]不存在", entityName));
        }
        List<SwEntityField> fields = getFieldsByEntityIdNoCheck(entities.get(0).getId());
        return convertToJSONObjects(fields);
    }

    @Override
    public JSONObject getEntityWithFieldsById(Long id) {
        SwEntity entity = entityMapper.selectByPrimaryKey(id);
        if (entity == null) {
            return convertToJSONObject(null);
        }
        JSONObject result = convertToJSONObject(entity);
        result.put("fields", convertToJSONObjects(getFieldsByEntityIdNoCheck(id)));

        return result;
    }

    @Override
    public JSONObject getEntityWithFieldsByName(String name) {
        SwEntityExample entityExample =  new SwEntityExample();
        entityExample.createCriteria().andNameEqualTo(name);
        List<SwEntity> entities = entityMapper.selectByExampleWithBLOBs(entityExample);
        if (CollectionUtils.isEmpty(entities)) {
            return convertToJSONObject(null);
        }
        SwEntity swEntity = entities.get(0);
        JSONObject result = convertToJSONObject(swEntity);
        result.put("fields", convertToJSONObjects(getFieldsByEntityIdNoCheck(swEntity.getId())));

        return result;
    }

    private List<SwEntityField> getFieldsByEntityIdNoCheck(Long entityId) {
        SwEntityFieldExample example = new SwEntityFieldExample();
        example.createCriteria().andEntityIdEqualTo(entityId);
        return entityFieldMapper.selectByExampleWithBLOBs(example);
    }

    @Override
    @Transactional
    public int deleteEntityById(Long id) throws Exception {
        SwEntity entity = entityMapper.selectByPrimaryKey(id);
        if (entity == null) {
            return 0;
        }
        if (hasBuildInEntity(Collections.singletonList(entity))) {
            throw new ParamException("内置实体不允许删除");
        }

        esIndexOperator.deleteIndexByAlias(entity.getTableName(), entity.getTableAlias());

        SwEntityFieldExample example = new SwEntityFieldExample();
        example.createCriteria().andEntityIdEqualTo(id);
        entityFieldMapper.deleteByExample(example);

        return entityMapper.deleteByPrimaryKey(id);
    }

    @Override
    @Transactional
    public int deleteEntityByName(String name) throws Exception {
        SwEntityExample example =  new SwEntityExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwEntity> entities = entityMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(entities)) {
            return 0;
        }

        SwEntity entity = entities.get(0);
        if (hasBuildInEntity(Collections.singletonList(entity))) {
            throw new ParamException("内置实体不允许删除");
        }
        esIndexOperator.deleteIndexByAlias(entity.getTableName(), entity.getTableAlias());

        SwEntityFieldExample fieldExample = new SwEntityFieldExample();
        fieldExample.createCriteria().andEntityIdEqualTo(entity.getId());
        entityFieldMapper.deleteByExample(fieldExample);

        return entityMapper.deleteByExample(example);
    }

    @Override
    public int deleteFieldById(Long entityId, Long fieldId) throws Exception {
        // 仅仅删除字段定义, 不修改ES存储数据
        SwEntityField entityField = entityFieldMapper.selectByPrimaryKey(fieldId);
        if (entityField != null) {
            if (hasBuildInEntityFiled(Collections.singletonList(entityField))) {
                throw new ParamException("内置实体字段不允许删除");
            }
            return deleteFieldByName(entityId, entityField.getField());
        }
        return 0;
    }

    @Override
    public int deleteFieldByName(Long entityId, String fieldName) throws Exception {
        // 仅仅删除字段定义, 不修改ES存储数据
        SwEntityFieldExample fieldExample = new SwEntityFieldExample();
        fieldExample.createCriteria().andEntityIdEqualTo(entityId).andFieldEqualTo(fieldName);
        List<SwEntityField> swEntityFields = entityFieldMapper.selectByExample(fieldExample);
        if (hasBuildInEntityFiled(swEntityFields)) {
            throw new ParamException("内置实体字段不允许删除");
        }
        return entityFieldMapper.deleteByExample(fieldExample);
    }

    @Override
    @Transactional
    public long createEntity(EntityCreateReq req) throws Exception {
        if (!getEntityByName(req.getName()).isEmpty()) {
            throw new EntityExistException(String.format("同名实体[%s]已存在", req.getName()));
        }

        SwEntity swEntity = buildSwEntity(req);
        entityMapper.insert(swEntity);

        EntityFieldCreateReq fieldCreateReq = new EntityFieldCreateReq();
        fieldCreateReq.setField(DwConstant.PARTITION_FIELD);
        fieldCreateReq.setDim(DwConstant.PARTITION_DIM);
        fieldCreateReq.setType(DwConstant.PARTITION_TYPE);
        fieldCreateReq.setBuildIn(DwConstant.PARTITION_BUILD_IN);
        fieldCreateReq.setAlias(DwConstant.PARTITION_ALIAS);
        fieldCreateReq.setNullable(DwConstant.PARTITION_NULLABLE);
        fieldCreateReq.setDescription(DwConstant.PARTITION_DESCRIPTION);

        SwEntityField swEntityField = buildSwEntityField(swEntity.getId(), fieldCreateReq);
        entityFieldMapper.insert(swEntityField);

        createTableMeta(swEntity.getTableName(), swEntity.getTableAlias(), swEntity.getLifecycle());

        return swEntity.getId();
    }

    @Override
    @Transactional
    public long createEntityWithFields(EntityCreateReq req, List<EntityFieldCreateReq> fieldReqs) throws Exception {
        boolean existId = fieldReqs.stream().anyMatch(fieldReq -> DwConstant.PRIMARY_FIELD.equals(fieldReq.getField()));
        if (!existId) {
            throw new ParamException("实体字段主键缺失, 需要定义id键字段");
        }

        long id = createEntity(req);
        for (EntityFieldCreateReq fieldCreateReq : fieldReqs) {
            addFieldByEntityId(id, fieldCreateReq);
        }

        return id;
    }

    @Override
    @Transactional
    public long updateEntity(EntityUpdateReq req) throws Exception {
        SwEntity oldEntity = entityMapper.selectByPrimaryKey(req.getId());
        if (oldEntity == null) {
            throw new EntityNotExistException(String.format("实体[id:%s]不存在", req.getId()));
        }

        if (hasBuildInEntity(Collections.singletonList(oldEntity))) {
            throw new ParamException("内置实体不允许修改");
        }

        SwEntity updateEntity = new SwEntity();
        updateEntity.setId(req.getId());
        updateEntity.setAlias(req.getAlias());
        updateEntity.setGmtModified(new Date());
        updateEntity.setLifecycle(req.getLifecycle());
        updateEntity.setDescription(req.getDescription());

        entityMapper.updateByPrimaryKeySelective(updateEntity);

        if (!oldEntity.getLifecycle().equals(req.getLifecycle())) {
            updateTableLifecycle(oldEntity.getTableName(), oldEntity.getTableAlias(), req.getLifecycle());
        }

        return oldEntity.getId();
    }

    @Override
    public int addFieldByEntityId(Long entityId, EntityFieldCreateReq req) throws Exception {
        if (DwConstant.PARTITION_FIELD.equals(req.getField())) {
            throw new EntityFieldException(String.format("默认分区列冲突,%s", DwConstant.PARTITION_FIELD));
        }

        SwEntity entity = entityMapper.selectByPrimaryKey(entityId);
        if (entity == null) {
            throw new EntityNotExistException(String.format("实体[id:%s]不存在", entityId));
        }

        SwEntityFieldExample fieldExample = new SwEntityFieldExample();
        fieldExample.createCriteria().andEntityIdEqualTo(entityId). andFieldEqualTo(req.getField());
        if (CollectionUtils.isEmpty(entityFieldMapper.selectByExample(fieldExample))) {
            SwEntityField swEntityField = buildSwEntityField(entityId, req);
            return entityFieldMapper.insert(swEntityField);
        } else {
            throw new EntityFieldExistException(String.format("实体[id:%s] 列[%s]已经存在", entityId, req.getField()));
        }
    }

    @Override
    public int updateFieldByEntityId(Long entityId, EntityFieldUpdateReq req) throws Exception {
        SwEntityField oldEntityField = entityFieldMapper.selectByPrimaryKey(req.getId());
        if (oldEntityField != null) {
            if (DwConstant.PARTITION_FIELD.equals(oldEntityField.getField())) {
                throw new EntityFieldException(String.format("默认分区列冲突,%s", DwConstant.PARTITION_FIELD));
            }

            if (hasBuildInEntityFiled(Collections.singletonList(oldEntityField))) {
                throw new ParamException("内置实体列不允许修改");
            }

            SwEntityField updateEntityField = buildSwEntityField(entityId, req);
            updateEntityField.setGmtCreate(null);
            updateEntityField.setId(req.getId());
            return entityFieldMapper.updateByPrimaryKeySelective(updateEntityField);
        }
        return 0;
    }

    private SwEntity buildSwEntity(EntityBaseReq req) {
        SwEntity swEntity = new SwEntity();
        Date now = new Date();
        swEntity.setGmtCreate(now);
        swEntity.setGmtModified(now);
        swEntity.setName(req.getName());
        swEntity.setAlias(req.getAlias());
        swEntity.setLayer(req.getLayer());
        swEntity.setBuildIn(req.getBuildIn());
        swEntity.setPartitionFormat(req.getPartitionFormat());
        swEntity.setLifecycle(req.getLifecycle());
        swEntity.setIcon(req.getIcon());
        swEntity.setDescription(req.getDescription());

        String tableAlias = req.getLayer()+ "_" + req.getName().toLowerCase();
        swEntity.setTableName("<" + tableAlias + "_" + DwConstant.INDEX_DATE_MATH.getString(req.getPartitionFormat()) + ">");
        swEntity.setTableAlias(tableAlias);

        return swEntity;
    }

    private SwEntityField buildSwEntityField(Long entityId, EntityFieldBaseReq req) {
        SwEntityField swEntityField = new SwEntityField();
        Date now = new Date();
        swEntityField.setGmtCreate(now);
        swEntityField.setGmtModified(now);
        swEntityField.setField(req.getField());
        swEntityField.setAlias(req.getAlias());
        swEntityField.setEntityId(entityId);
        swEntityField.setDim(req.getDim());
        swEntityField.setBuildIn(req.getBuildIn());
        swEntityField.setType(req.getType().getType());
        swEntityField.setNullable(req.getNullable());
        swEntityField.setDescription(req.getDescription());

        return swEntityField;
    }

    private void addTableField() {
        // TODO 按照数据类型做mapping映射
    }

    private boolean hasBuildInEntity(List<SwEntity> entities) {
        Optional<SwEntity> optional = entities.stream().filter(SwEntity::getBuildIn).findAny();
        return optional.isPresent();
    }

    private boolean hasBuildInEntityFiled(List<SwEntityField> entityFields) {
        Optional<SwEntityField> optional = entityFields.stream().filter(SwEntityField::getBuildIn).findAny();
        return optional.isPresent();
    }
}
