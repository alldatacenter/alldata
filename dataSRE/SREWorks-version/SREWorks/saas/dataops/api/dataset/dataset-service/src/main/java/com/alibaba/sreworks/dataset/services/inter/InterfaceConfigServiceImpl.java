package com.alibaba.sreworks.dataset.services.inter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.InterfaceConfigService;
import com.alibaba.sreworks.dataset.common.exception.InterfaceExistException;
import com.alibaba.sreworks.dataset.common.exception.InterfaceNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfig;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfigExample;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfigMapper;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据接口Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/12/07 17:18
 */
@Slf4j
@Service
public class InterfaceConfigServiceImpl implements InterfaceConfigService {
    @Autowired
    InterfaceConfigMapper configMapper;

    @Override
    public JSONObject getConfigById(Integer interfaceId) {
        InterfaceConfig interfaceConfig = configMapper.selectByPrimaryKey(interfaceId);
        return convertToJSONObject(interfaceConfig);
    }

    @Override
    public JSONObject getConfigByName(String name) {
        InterfaceConfigExample example = new InterfaceConfigExample();
        example.createCriteria().andNameEqualTo(name);
        List<InterfaceConfig> interfaceConfigs = configMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(interfaceConfigs)) {
            return convertToJSONObject(null);
        }
        return convertToJSONObject(interfaceConfigs.get(0));
    }

    @Override
    public List<JSONObject> getConfigs() {
        List<InterfaceConfig> interfaceConfigs = configMapper.selectByExampleWithBLOBs(new InterfaceConfigExample());
        return convertToJSONObjects(interfaceConfigs);
    }

    @Override
    public boolean existInterface(Integer interfaceId) {
        return configMapper.selectByPrimaryKey(interfaceId) != null;
    }

    @Override
    public boolean buildInInterface(Integer interfaceId) {
        InterfaceConfig interfaceConfig = configMapper.selectByPrimaryKey(interfaceId);
        return interfaceConfig != null && interfaceConfig.getBuildIn();
    }

    @Override
    @Transactional
    public int deleteConfigById(Integer interfaceId) throws Exception {
        if (buildInInterface(interfaceId)) {
            throw new ParamException("内置模型不允许删除");
        }

        return configMapper.deleteByPrimaryKey(interfaceId);
    }

    @Override
    public int addConfig(InterfaceConfigCreateReq req) throws Exception {
        if (!CollectionUtils.isEmpty(getConfigByName(req.getName()))) {
            throw new InterfaceExistException(String.format("同名接口[%s]已存在", req.getName()));
        }

        InterfaceConfig config = new InterfaceConfig();
        Date now = new Date();
        config.setGmtCreate(now);
        config.setGmtModified(now);
        config.setName(req.getName());
        config.setAlias(req.getAlias());
        config.setDataSourceType(req.getDataSourceType());
        config.setDataSourceId(req.getDataSourceId());
        config.setDataSourceTable(req.getDataSourceTable());
        config.setMode(req.getMode());
        config.setQlTemplate(req.getQlTemplate());
        config.setBuildIn(req.getBuildIn());
        config.setCreator(req.getCreator());
        config.setLastModifier(req.getLastModifier());
        config.setRequestMethod(req.getRequestMethod());
        config.setContentType(req.getContentType());
        config.setPaging(req.getPaging());

        config.setQueryFields(req.getQueryFieldsJSONString());
        config.setGroupFields(req.getGroupFieldsJSONString());
        config.setSortFields(req.getSortFieldsJSONString());
        config.setRequestParams(req.getRequestParamsJSONString());
        config.setResponseParams(req.getResponseParamsJSONString());

        configMapper.insert(config);

        return config.getId();
    }

    @Override
    public int updateConfig(InterfaceConfigUpdateReq req) throws Exception {
        InterfaceConfig interfaceConfig = configMapper.selectByPrimaryKey(req.getId());
        if (interfaceConfig == null) {
            throw new InterfaceNotExistException("数据接口不存在");
        }
        if (interfaceConfig.getBuildIn()) {
            throw new ParamException("内置模型不允许修改");
        }

        InterfaceConfig config = new InterfaceConfig();
        Date now = new Date();
        config.setId(req.getId());
        config.setGmtModified(now);
        config.setName(req.getName());
        config.setAlias(req.getAlias());
        config.setDataSourceType(req.getDataSourceType());
        config.setDataSourceId(req.getDataSourceId());
        config.setDataSourceTable(req.getDataSourceTable());
        config.setMode(req.getMode());
        config.setQlTemplate(req.getQlTemplate());
        config.setLastModifier(req.getLastModifier());
        config.setRequestMethod(req.getRequestMethod());
        config.setContentType(req.getContentType());
        config.setPaging(req.getPaging());

        config.setQueryFields(req.getQueryFieldsJSONString());
        config.setGroupFields(req.getGroupFieldsJSONString());
        config.setSortFields(req.getSortFieldsJSONString());
        config.setRequestParams(req.getRequestParamsJSONString());
        config.setResponseParams(req.getResponseParamsJSONString());

        configMapper.updateByPrimaryKeySelective(config);

        return config.getId();
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

        String groupFieldsStr = result.getString("groupFields");
        if (StringUtils.isNotEmpty(groupFieldsStr)) {
            result.put("groupFields", JSONArray.parseArray(groupFieldsStr));
        }

        String sortFieldsStr = result.getString("sortFields");
        if (StringUtils.isNotEmpty(sortFieldsStr)) {
            result.put("sortFields", JSONArray.parseArray(sortFieldsStr));
        }

        String requestParamsStr = result.getString("requestParams");
        if (StringUtils.isNotEmpty(requestParamsStr)) {
            result.put("requestParams", JSONArray.parseArray(requestParamsStr));
        }

        String responseParamsStr = result.getString("responseParams");
        if (StringUtils.isNotEmpty(responseParamsStr)) {
            result.put("responseParams", JSONArray.parseArray(responseParamsStr));
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
