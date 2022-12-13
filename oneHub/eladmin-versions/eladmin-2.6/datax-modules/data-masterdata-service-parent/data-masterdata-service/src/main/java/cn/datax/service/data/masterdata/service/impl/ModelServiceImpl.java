package cn.datax.service.data.masterdata.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.MsgFormatUtil;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.masterdata.api.entity.ModelColumnEntity;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import cn.datax.service.data.masterdata.api.dto.ModelDto;
import cn.datax.service.data.masterdata.dao.ModelColumnDao;
import cn.datax.service.data.masterdata.dao.MysqlDynamicDao;
import cn.datax.service.data.masterdata.mapstruct.ModelMapstruct;
import cn.datax.service.data.masterdata.service.ModelService;
import cn.datax.service.data.masterdata.dao.ModelDao;
import cn.datax.common.base.BaseServiceImpl;
import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.workflow.api.dto.ProcessInstanceCreateRequest;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import cn.datax.service.workflow.api.feign.FlowInstanceServiceFeign;
import cn.datax.service.workflow.api.vo.FlowInstanceVo;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 主数据模型表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ModelServiceImpl extends BaseServiceImpl<ModelDao, ModelEntity> implements ModelService {

    @Autowired
    private ModelDao modelDao;

    @Autowired
    private ModelMapstruct modelMapstruct;

    @Autowired
    private ModelColumnDao modelColumnDao;

    @Autowired
    private MysqlDynamicDao dynamicDao;

    @Autowired
    private RedisService redisService;

    @Autowired
    private FlowInstanceServiceFeign flowInstanceServiceFeign;

    private static String BIND_GB_CODE = "gb_code";
    private static String BIND_GB_NAME = "gb_name";

    private static String DEFAULT_BUSINESS_CODE = "5011";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelEntity saveModel(ModelDto modelDto) {
        ModelEntity model = modelMapstruct.toEntity(modelDto);
        model.setIsSync(DataConstant.TrueOrFalse.FALSE.getKey());
        model.setModelPhysicalTable("dynamic_" + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
        modelDao.insert(model);
        String modelId = model.getId();
        List<ModelColumnEntity> modelColumns = model.getModelColumns();
        if(CollUtil.isNotEmpty(modelColumns)){
            modelColumns.forEach(c -> {
                c.setModelId(modelId);
                modelColumnDao.insert(c);
            });
        }
        return model;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelEntity updateModel(ModelDto modelDto) {
        ModelEntity model = modelMapstruct.toEntity(modelDto);
        modelDao.updateById(model);
        String modelId = model.getId();
        modelColumnDao.delete(Wrappers.<ModelColumnEntity>lambdaQuery()
                .eq(ModelColumnEntity::getModelId, modelId));
        List<ModelColumnEntity> modelColumns = model.getModelColumns();
        if(CollUtil.isNotEmpty(modelColumns)){
            modelColumns.forEach(c -> {
                c.setModelId(modelId);
                modelColumnDao.insert(c);
            });
        }
        return model;
    }

    @Override
    public ModelEntity getModelById(String id) {
        ModelEntity modelEntity = super.getById(id);
        return modelEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelById(String id) {
        modelColumnDao.delete(Wrappers.<ModelColumnEntity>lambdaQuery()
                .eq(ModelColumnEntity::getModelId, id));
        modelDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelBatch(List<String> ids) {
        modelColumnDao.delete(Wrappers.<ModelColumnEntity>lambdaQuery()
                .in(ModelColumnEntity::getModelId, ids));
        modelDao.deleteBatchIds(ids);
    }

    @Override
    public void createTable(String id) {
        ModelEntity modelEntity = super.getById(id);
        if (DataConstant.TrueOrFalse.TRUE.getKey().equals(modelEntity.getIsSync())) {
            throw new DataException("重复建模");
        }
        dynamicDao.createTable(modelEntity);
        modelEntity.setIsSync(DataConstant.TrueOrFalse.TRUE.getKey());
        modelDao.updateById(modelEntity);
    }

    @Override
    public void dropTable(String id) {
        ModelEntity modelEntity = super.getById(id);
        String tableName = modelEntity.getModelPhysicalTable();
        dynamicDao.dropTable(tableName);
    }

    @Override
    public Map<String, Object> getTableParamById(String id) {
        ModelEntity modelEntity = super.getById(id);
        String tableName = modelEntity.getModelPhysicalTable();
        List<ModelColumnEntity> modelColumns = modelEntity.getModelColumns();
        // 列表展示字段
        List<Map<String, Object>> columnList = modelColumns.stream().filter(s -> DataConstant.TrueOrFalse.TRUE.getKey().equals(s.getIsList())).map(s -> {
            Map<String, Object> map = new HashMap<>(4);
            map.put("prop", s.getColumnName());
            map.put("label", s.getColumnComment());
            return map;
        }).collect(Collectors.toList());
        // 查询参数字段
        List<Map<String, Object>> queryList = modelColumns.stream().filter(s -> DataConstant.TrueOrFalse.TRUE.getKey().equals(s.getIsQuery())).map(s -> {
            Map<String, Object> map = new HashMap<>(4);
            map.put("column", s.getColumnName());
            map.put("columnName", s.getColumnComment());
            map.put("columnType", s.getColumnType());
            map.put("columnScale", s.getColumnScale());
            map.put("queryType", s.getQueryType());
            map.put("htmlType", s.getHtmlType());
            if (DataConstant.TrueOrFalse.TRUE.getKey().equals(s.getIsBindDict()) && StrUtil.isNotBlank(s.getBindDictTypeId())) {
                String bindDictColumn = s.getBindDictColumn();
                List<DictEntity> dictList = (List<DictEntity>) redisService.hget(RedisConstant.STANDARD_DICT_KEY, s.getBindDictTypeId());
                List<Map<String, Object>> mapList = dictList.stream().map(d -> {
                    Map<String, Object> dictMap = new HashMap<>(4);
                    dictMap.put("id", d.getId());
                    dictMap.put("value", BIND_GB_CODE.equals(bindDictColumn) ? d.getGbCode() : d.getGbName());
                    dictMap.put("label", d.getGbName());
                    return dictMap;
                }).collect(Collectors.toList());
                map.put("dictList", mapList);
            }
            return map;
        }).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>(4);
        map.put("modelId", id);
        map.put("tableName", tableName);
        map.put("columnList", columnList);
        map.put("queryList", queryList);
        return map;
    }

    @Override
    public Map<String, Object> getFormParamById(String id) {
        ModelEntity modelEntity = super.getById(id);
        String tableName = modelEntity.getModelPhysicalTable();
        List<ModelColumnEntity> modelColumns = modelEntity.getModelColumns();
        List<Map<String, Object>> columnList = modelColumns.stream().filter(s -> DataConstant.TrueOrFalse.FALSE.getKey().equals(s.getIsSystem())).map(s -> {
            Map<String, Object> map = new HashMap<>(16);
            map.put("id", s.getId());
            map.put("columnName", s.getColumnName());
            map.put("columnComment", s.getColumnComment());
            map.put("columnType", s.getColumnType());
            map.put("columnLength", s.getColumnLength());
            map.put("columnScale", s.getColumnScale());
            map.put("defaultValue", s.getDefaultValue());
            map.put("isRequired", s.getIsRequired());
            map.put("isInsert", s.getIsInsert());
            map.put("isEdit", s.getIsEdit());
            map.put("isDetail", s.getIsEdit());
            map.put("isList", s.getIsList());
            map.put("isQuery", s.getIsQuery());
            map.put("queryType", s.getQueryType());
            map.put("isBindDict", s.getIsBindDict());
            if (DataConstant.TrueOrFalse.TRUE.getKey().equals(s.getIsBindDict()) && StrUtil.isNotBlank(s.getBindDictTypeId())) {
                String bindDictColumn = s.getBindDictColumn();
                List<DictEntity> dictList = (List<DictEntity>) redisService.hget(RedisConstant.STANDARD_DICT_KEY, s.getBindDictTypeId());
                List<Map<String, Object>> mapList = dictList.stream().map(d -> {
                    Map<String, Object> dictMap = new HashMap<>(4);
                    dictMap.put("id", d.getId());
                    dictMap.put("value", BIND_GB_CODE.equals(bindDictColumn) ? d.getGbCode() : d.getGbName());
                    dictMap.put("label", d.getGbName());
                    return dictMap;
                }).collect(Collectors.toList());
                map.put("dictList", mapList);
            }
            map.put("htmlType", s.getHtmlType());
            return map;
        }).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>(4);
        map.put("modelId", id);
        map.put("tableName", tableName);
        map.put("columnList", columnList);
        return map;
    }

    @Override
    public void submitModelById(String id) {
        BusinessEntity businessEntity = (BusinessEntity) redisService.hget(RedisConstant.WORKFLOW_BUSINESS_KEY, DEFAULT_BUSINESS_CODE);
        if (businessEntity != null) {
            ProcessInstanceCreateRequest request = new ProcessInstanceCreateRequest();
            request.setSubmitter(SecurityUtil.getUserId());
            request.setBusinessKey(id);
            request.setBusinessCode(DEFAULT_BUSINESS_CODE);
            request.setBusinessAuditGroup(businessEntity.getBusinessAuditGroup());
            String processDefinitionId = businessEntity.getProcessDefinitionId();
            request.setProcessDefinitionId(processDefinitionId);
            // 流程实例标题(动态拼接)
            String tempalte = businessEntity.getBusinessTempalte();
            String businessName = businessEntity.getBusinessName();
            Map<String, String> parameters = new HashMap<>(4);
            parameters.put(MsgFormatUtil.TEMPALTE_NICKNAME, SecurityUtil.getNickname());
            parameters.put(MsgFormatUtil.TEMPALTE_DATETIME, DateUtil.formatLocalDateTime(LocalDateTime.now()));
            parameters.put(MsgFormatUtil.TEMPALTE_BUSINESS_NAME, businessName);
            parameters.put(MsgFormatUtil.TEMPALTE_BUSINESS_KEY, id);
            String content = MsgFormatUtil.getContent(tempalte, parameters);
            request.setBusinessName(content);
            FlowInstanceVo flowInstanceVo = flowInstanceServiceFeign.startById(request);
            if (flowInstanceVo != null) {
                ModelEntity modelEntity = new ModelEntity();
                modelEntity.setId(id);
                modelEntity.setProcessInstanceId(flowInstanceVo.getProcessInstanceId());
                modelDao.updateById(modelEntity);
            }
        } else {
            throw new DataException("业务流程未配置");
        }
    }
}
