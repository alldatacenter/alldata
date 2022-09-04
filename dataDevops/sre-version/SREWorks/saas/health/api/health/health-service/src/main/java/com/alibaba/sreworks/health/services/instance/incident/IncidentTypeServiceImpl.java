package com.alibaba.sreworks.health.services.instance.incident;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.incident.IncidentTypeService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.IncidentTypeDeleteException;
import com.alibaba.sreworks.health.common.exception.IncidentTypeExistException;
import com.alibaba.sreworks.health.common.exception.IncidentTypeNotExistException;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.bo.IncidentExConfig;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeBaseReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeUpdateReq;
import com.alibaba.sreworks.health.services.cache.HealthDomainCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 异常类型
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/20 11:40
 */
@Slf4j
@Service
public class IncidentTypeServiceImpl implements IncidentTypeService {

    @Autowired
    IncidentTypeMapper incidentTypeMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    HealthDomainCacheService domainCacheService;

    @Override
    public JSONObject getIncidentTypeById(Integer id) {
        return convertToJSONObject(incidentTypeMapper.selectByPrimaryKey(id));
    }

    @Override
    public JSONObject getIncidentTypeByLabel(String label) {
        IncidentTypeExample example = new IncidentTypeExample();
        example.createCriteria().andLabelEqualTo(label);

        List<IncidentType> incidentTypes = incidentTypeMapper.selectByExample(new IncidentTypeExample());
        if (CollectionUtils.isEmpty(incidentTypes)) {
            return convertToJSONObject(null);
        }
        return convertToJSONObject(incidentTypes.get(0));
    }

    @Override
    public List<JSONObject> getIncidentTypes() {
        return convertToJSONObjects(incidentTypeMapper.selectByExample(new IncidentTypeExample()));
    }

    @Override
    public boolean existIncidentType(Integer id) {
        return incidentTypeMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistIncidentType(Integer id) {
        return !existIncidentType(id);
    }

    @Override
    public int addIncidentType(IncidentTypeCreateReq req) throws Exception {
        if (!CollectionUtils.isEmpty(getIncidentTypeByLabel(req.getLabel()))) {
            throw new IncidentTypeExistException(String.format("异常类型[%s]已存在", req.getLabel()));
        }

        IncidentType type = buildIncidentType(req);
        type.setLastModifier(type.getCreator());
        int result = incidentTypeMapper.insert(type);

        domainCacheService.expireKey(Constant.CACHE_INCIDENT_TYPE_KEY);

        return result;
    }

    @Override
    public int updateIncidentType(IncidentTypeUpdateReq req) throws Exception {
        IncidentType existIncidentType = incidentTypeMapper.selectByPrimaryKey(req.getId());
        if (existIncidentType == null) {
            throw new IncidentTypeNotExistException(String.format("更新异常类型[id:%s]不存在", req.getId()));
        }

        if (StringUtils.isEmpty(req.getLabel())) {
            req.setLabel(existIncidentType.getLabel());
        }

        IncidentType type = buildIncidentType(req);
        type.setId(req.getId());
        type.setGmtCreate(null);
        int result = incidentTypeMapper.updateByPrimaryKeySelective(type);

        domainCacheService.expireKey(Constant.CACHE_INCIDENT_TYPE_KEY);

        return result;
    }

    @Override
    @Transactional
    public int deleteIncidentType(Integer id) throws Exception {
        IncidentType incidentType = incidentTypeMapper.selectByPrimaryKey(id);
        if (incidentType == null) {
            return 0;
        }

        CommonDefinitionExample definitionExample = new CommonDefinitionExample();
        definitionExample.createCriteria().andCategoryEqualTo(Constant.INCIDENT);
        List<CommonDefinition> commonDefinitions = definitionMapper.selectByExample(definitionExample);

        Optional<CommonDefinition> optDefinition = commonDefinitions.stream().filter(definition -> {
            IncidentExConfig exConfig = JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class);
            return exConfig.getTypeId().equals(id);
        }).findAny();
        if (optDefinition.isPresent()) {
            CommonDefinition usedDefinition = optDefinition.get();
            throw new IncidentTypeDeleteException(String.format("异常类型[id:%s]删除失败, 被异常定义[id:%s, name:%s]引用", id, usedDefinition.getId(), usedDefinition.getName()));
        }

        int result = incidentTypeMapper.deleteByPrimaryKey(id);

        domainCacheService.expireKey(Constant.CACHE_INCIDENT_TYPE_KEY);

        return result;
    }

    private IncidentType buildIncidentType(IncidentTypeBaseReq req) {

        IncidentType type = new IncidentType();
        Date now = new Date();
        type.setGmtCreate(now);
        type.setGmtModified(now);
        type.setLabel(req.getLabel());
        type.setName(req.getName());
        String mqTopic = Constant.APP_NAME + "_" + req.getLabel();
        type.setMqTopic(mqTopic);
        type.setCreator(req.getCreator());
        type.setLastModifier(req.getLastModifier());
        type.setDescription(req.getDescription());

        return type;
    }
}
