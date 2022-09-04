package com.alibaba.sreworks.warehouse.services.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.data.DwDataService;
import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.ModelFieldException;
import com.alibaba.sreworks.warehouse.common.exception.ModelNotExistException;
import com.alibaba.sreworks.warehouse.common.exception.ParamException;
import com.alibaba.sreworks.warehouse.domain.*;
import com.alibaba.sreworks.warehouse.operator.ESDocumentOperator;
import com.alibaba.sreworks.warehouse.operator.ESIndexOperator;
import com.alibaba.sreworks.warehouse.services.DwCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
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
public class ModelDataServiceImpl extends DwCommonService implements DwDataService {

    @Autowired
    SwModelMapper swModelMapper;

    @Autowired
    SwModelFieldMapper swModelFieldMapper;

    @Autowired
    ESIndexOperator esIndexOperator;

    @Autowired
    ESDocumentOperator esDocumentOperator;

    @Override
    public int flushDwData(Long id, JSONObject data) throws Exception {
        return flushDwDatas(id, Collections.singletonList(data));
    }

    @Override
    public int flushDwDatas(Long id, List<JSONObject> datas) throws Exception {
        if (CollectionUtils.isEmpty(datas)) {
            return 0;
        } else if (datas.size() > Constant.MAX_MODEL_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_MODEL_DATA_FLUSH_SIZE));
        }

        SwModel swModel = swModelMapper.selectByPrimaryKey(id);
        if (swModel == null) {
            throw new ModelNotExistException(String.format("数据模型不存在,请检查参数,模型[ID:%s]", id));
        }

        JSONObject modelFields = getModelFields(id);
        if (!modelFields.containsKey(DwConstant.PRIMARY_FIELD)) {
            throw new ModelFieldException(String.format("数据模型字段缺失id,请补充模型主键,模型[ID:%s]", id));
        }

        List<JSONObject> esDatas = datas.parallelStream().map(data -> convertToESData(data, swModel.getPartitionFormat(), modelFields)).filter(data -> !data.isEmpty()).collect(Collectors.toList());
        return doFlushDatas(swModel.getTableAlias(), swModel.getTableName(), swModel.getPartitionFormat(), swModel.getLifecycle(), esDatas);
    }

    @Override
    public int flushDwData(String name, JSONObject data) throws Exception {
        return flushDwDatas(name, Collections.singletonList(data));
    }

    @Override
    public int flushDwDatas(String name, List<JSONObject> datas) throws Exception {
        if (CollectionUtils.isEmpty(datas)) {
            return 0;
        } else if (datas.size() > Constant.MAX_MODEL_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_MODEL_DATA_FLUSH_SIZE));
        }

        SwModelExample example = new SwModelExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwModel> swModels = swModelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(swModels)) {
            throw new ModelNotExistException(String.format("数据模型不存在,请检查参数,模型[name:%s]", name));
        }
        return flushDwDatas(swModels.get(0).getId(), datas);
    }

    private JSONObject getModelFields(Long modelId) {
        JSONObject modelFields = new JSONObject();
        SwModelFieldExample example = new SwModelFieldExample();
        example.createCriteria().andModelIdEqualTo(modelId);
        List<SwModelField> swModelFields = swModelFieldMapper.selectByExampleWithBLOBs(example);
        swModelFields.forEach(swModelField -> modelFields.put(swModelField.getField(), swModelField));
        return modelFields;
    }
}
