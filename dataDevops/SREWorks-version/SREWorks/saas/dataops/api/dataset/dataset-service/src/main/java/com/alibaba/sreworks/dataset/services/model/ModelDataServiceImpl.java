package com.alibaba.sreworks.dataset.services.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.dataset.api.model.ModelDataService;
import com.alibaba.sreworks.dataset.common.ESDocumentOperator;
import com.alibaba.sreworks.dataset.common.ESIndexOperator;
import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.exception.ModelNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.common.type.ColumnType;
import com.alibaba.sreworks.dataset.common.utils.Tools;
import com.alibaba.sreworks.dataset.domain.bo.DataModelField;
import com.alibaba.sreworks.dataset.domain.primary.DataModelConfigMapper;
import com.alibaba.sreworks.dataset.domain.primary.DataModelConfigWithBLOBs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型数据
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/09 16:28
 */
@Slf4j
@Service
public class ModelDataServiceImpl implements ModelDataService {

    @Autowired
    DataModelConfigMapper dataModelConfigMapper;

    @Autowired
    ESIndexOperator esIndexOperator;

    @Autowired
    ESDocumentOperator esDocumentOperator;

    @Override
    public int flushModelData(Integer modelId, JSONObject data) throws Exception {
        return flushModelDatas(modelId, Arrays.asList(data));
    }

    @Override
    public int flushModelDatas(Integer modelId, List<JSONObject> datas) throws Exception {
        int result = 0;

        if (CollectionUtils.isEmpty(datas)) {
            return result;
        } else if (datas.size() > Constant.MAX_MODEL_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_MODEL_DATA_FLUSH_SIZE));
        }

        DataModelConfigWithBLOBs modelConfig = dataModelConfigMapper.selectByPrimaryKey(modelId);
        if (modelConfig == null ) {
            throw new ModelNotExistException(String.format("数据模型不存在,请检查参数,模型ID:%s", modelId));
        }



        String sourceType = modelConfig.getSourceType();
        if (!sourceType.equals(ValidConstant.ES_SOURCE)) {
            throw new ParamException("当前仅支持ES数据源的模型进行数据落盘");
        }

        JSONObject modelFields = parseModelFields(modelConfig);
        List<JSONObject> esDatas = datas.parallelStream().map(data -> convertToESData(data, modelFields)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(esDatas)) {
            return result;
        }

        String alias = modelConfig.getSourceTable();
        String index = new StringSubstitutor(
                new HashMap<String, String>(){
                    {
                        put("alias", alias);
                    }
                }).replace(Constant.INDEX_FORMAT);
        log.info(String.format("====flush data, es_index:%s, es_index_alias:%s====", index, alias));

        esIndexOperator.createIndexIfNotExist(index, alias, modelConfig.getSourceId());
        if (esDatas.size() > 1) {
            result = esDocumentOperator.upsertBulkJSON(index, modelConfig.getSourceId(), esDatas);
        } else {
            result = esDocumentOperator.upsertJSON(index, modelConfig.getSourceId(), esDatas.get(0));
        }

        return result;
    }

    private JSONObject parseModelFields(DataModelConfigWithBLOBs modelConfig) {
        JSONObject modelFields = new JSONObject();

        if (StringUtils.isNotEmpty(modelConfig.getValueFields())) {
            List<DataModelField> valueFields = JSONArray.parseArray(modelConfig.getValueFields(), DataModelField.class);
            valueFields.forEach(valueField -> modelFields.put(valueField.getField(), valueField));
        }

        if (StringUtils.isNotEmpty(modelConfig.getGroupFields())) {
            List<DataModelField> groupFields = JSONArray.parseArray(modelConfig.getGroupFields(), DataModelField.class);
            groupFields.forEach(groupField -> modelFields.put(groupField.getField(), groupField));
        }

        return modelFields;
    }

    private JSONObject convertToESData(JSONObject data, JSONObject modelFields) {
        JSONObject esData = new JSONObject();

        for(String fieldName : modelFields.keySet()) {
            DataModelField modelField = modelFields.getObject(fieldName, DataModelField.class);

            String key = modelField.getDim();
            Object value = data.get(fieldName);

            ColumnType fieldType = ColumnType.valueOf(modelField.getType().toUpperCase());
            switch (fieldType) {
                case BOOLEAN:
                    esData.put(key, value != null ? TypeUtils.castToBoolean(value) : null);
                    break;
                case INT:
                    esData.put(key, value != null ? TypeUtils.castToInt(value) : null);
                    break;
                case LONG:
                    esData.put(key, value != null ? TypeUtils.castToLong(value) : null);
                    break;
                case FLOAT:
                    esData.put(key, value != null ? TypeUtils.castToFloat(value) : null);
                    break;
                case DOUBLE:
                    esData.put(key, value != null ? TypeUtils.castToDouble(value) : null);
                    break;
                case STRING:
                    esData.put(key, TypeUtils.castToString(value));
                    break;
                case DATE:
                    if (value == null) {
                        esData.put(key, null);
                    } else if (value instanceof Number) {
                        Long number = ((Number)value).longValue();
                        if (number <= Constant.MAX_SECONDS_TIMESTAMP) {
                            esData.put(key, TypeUtils.castToDate(number, "unixtime"));
                        } else {
                            esData.put(key, TypeUtils.castToDate(number));
                        }
                    } else if (value instanceof String) {
                        esData.put(key, TypeUtils.castToDate(value));
                    } else {
                        esData.put(key, null);
                    }
                    break;
                case OBJECT: case ARRAY:
                    esData.put(key, Tools.richDoc(value));
                    break;
                default:
                    log.warn(String.format("请求模型列类型[%s]非法, 请仔细核对接口定义", fieldType));
            }

            // generate doc id
            if (fieldName.equals(Constant.ID)) {
                esData.put(Constant.META_ID, esData.get(key));
            }
        }
        return esData;
    }
}
