package com.alibaba.sreworks.warehouse.services.entity;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.data.DwDataService;
import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.EntityFieldException;
import com.alibaba.sreworks.warehouse.common.exception.EntityNotExistException;
import com.alibaba.sreworks.warehouse.common.exception.ParamException;
import com.alibaba.sreworks.warehouse.domain.*;
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
public class EntityDataServiceImpl extends DwCommonService implements DwDataService {

    @Autowired
    SwEntityMapper swEntityMapper;

    @Autowired
    SwEntityFieldMapper swEntityFieldMapper;

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

        SwEntity swEntity = swEntityMapper.selectByPrimaryKey(id);
        if (swEntity == null) {
            throw new EntityNotExistException(String.format("数据实体不存在,请检查参数,实体[ID:%s]", id));
        }

        JSONObject entityFields = getEntityFields(id);
        if (!entityFields.keySet().contains(DwConstant.PRIMARY_FIELD)) {
            throw new EntityFieldException(String.format("数据实体字段缺失id,请补充模型主键,实体[ID:%s]", id));
        }

        List<JSONObject> esDatas = datas.parallelStream().map(data -> convertToESData(data, swEntity.getPartitionFormat(), entityFields)).filter(data -> !data.isEmpty()).collect(Collectors.toList());
        return doFlushDatas(swEntity.getTableAlias(), swEntity.getTableName(), swEntity.getPartitionFormat(), swEntity.getLifecycle(), esDatas);
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

        SwEntityExample example = new SwEntityExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwEntity> swEntities = swEntityMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(swEntities)) {
            throw new EntityNotExistException(String.format("数据实体不存在,请检查参数,实体[name:%s]", name));
        }

        return flushDwDatas(swEntities.get(0).getId(), datas);
    }

    private JSONObject getEntityFields(Long id) {
        JSONObject entityFields = new JSONObject();
        SwEntityFieldExample example = new SwEntityFieldExample();
        example.createCriteria().andEntityIdEqualTo(id);
        List<SwEntityField> swEntityFields = swEntityFieldMapper.selectByExampleWithBLOBs(example);
        swEntityFields.forEach(swEntityField -> entityFields.put(swEntityField.getField(), swEntityField));
        return entityFields;
    }
}
