package com.alibaba.sreworks.health.services.definition;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.definition.DefinitionService;
import com.alibaba.sreworks.health.cache.IncidentDefCache;
import com.alibaba.sreworks.health.cache.IncidentTypeCache;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.*;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.bo.*;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionBaseReq;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionCreateReq;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionExConfigReq;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionUpdateReq;
import com.alibaba.sreworks.health.services.cache.HealthDomainCacheService;
import com.alibaba.sreworks.health.utils.DefExConfigValidator;
import com.google.common.collect.ImmutableList;
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
 * 运维事件定义Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 15:55
 */
@Slf4j
@Service
public class DefinitionServiceImpl implements DefinitionService {

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    IncidentTypeMapper incidentTypeMapper;

    @Autowired
    IncidentInstanceMapper incidentInstanceMapper;

    @Autowired
    RiskTypeMapper riskTypeMapper;

    @Autowired
    FailureInstanceMapper failureInstanceMapper;

    @Autowired
    RiskInstanceMapper riskInstanceMapper;

    @Autowired
    AlertInstanceMapper alertInstanceMapper;

    @Autowired
    EventInstanceMapper eventInstanceMapper;

    @Autowired
    HealthDomainCacheService domainCacheService;

    @Override
    public JSONObject getDefinitionsStat() {
        List<CommonDefinitionGroupCount> stats = definitionMapper.countGroupByCategory();
        JSONObject definitionStat = new JSONObject();
        stats.forEach(stat -> definitionStat.put(stat.getCategory(), stat.getCnt()));

        JSONObject totalInsStat = getTotalInstancesInc(null);

        JSONObject curDayInsStat = getCurrentDayInstancesInc(null);

        JSONObject result = new JSONObject();

        ImmutableList<String> categories = ImmutableList.of(Constant.RISK, Constant.ALERT, Constant.INCIDENT, Constant.FAILURE);
        for (String category : categories) {
            JSONObject item = new JSONObject();
            item.put("definition", definitionStat.getInteger(category));
            item.put("totalIns", totalInsStat.getLong(category));
            item.put("curDayIns", curDayInsStat.getLong(category));
            result.put(category, item);
        }

        return result;
    }

    @Override
    public JSONObject getInstancesStat(String appInstanceId) {
        JSONObject totalInsStat = getTotalInstancesInc(appInstanceId);
        JSONObject curDayInsStat = getCurrentDayInstancesInc(appInstanceId);

        JSONObject result = new JSONObject();
        ImmutableList<String> categories = ImmutableList.of(Constant.RISK, Constant.ALERT, Constant.INCIDENT, Constant.FAILURE);
        for (String category : categories) {
            JSONObject item = new JSONObject();
            item.put("totalIns", totalInsStat.getLong(category));
            item.put("curDayIns", curDayInsStat.getLong(category));
            result.put(category, item);
        }

        return result;
    }

    @Override
    public JSONObject getTotalInstancesInc(String appInstanceId) {
        RiskInstanceExample riskInstanceExample = new RiskInstanceExample();
        AlertInstanceExample alertInstanceExample = new AlertInstanceExample();
        IncidentInstanceExample incidentInstanceExample = new IncidentInstanceExample();
        FailureInstanceExample failureInstanceExample = new FailureInstanceExample();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            riskInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId);
            alertInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId);
            incidentInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId);
            failureInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId);
        }

        long riskInsCnt = riskInstanceMapper.countByExample(riskInstanceExample);
        long alertInsCnt = alertInstanceMapper.countByExample(alertInstanceExample);
        long incidentInsCnt = incidentInstanceMapper.countByExample(incidentInstanceExample);
        long failureInsCnt = failureInstanceMapper.countByExample(failureInstanceExample);

        JSONObject result = new JSONObject();
        result.put(Constant.RISK, riskInsCnt);
        result.put(Constant.ALERT, alertInsCnt);
        result.put(Constant.INCIDENT, incidentInsCnt);
        result.put(Constant.FAILURE, failureInsCnt);
        return result;
    }

    @Override
    public JSONObject getCurrentDayInstancesInc(String appInstanceId) {
        Date now = new Date();
        now.setTime(now.getTime() - now.getTime()%Constant.ONE_DAY_MILLISECOND);

        RiskInstanceExample riskInstanceExample = new RiskInstanceExample();
        AlertInstanceExample alertInstanceExample = new AlertInstanceExample();
        IncidentInstanceExample incidentInstanceExample = new IncidentInstanceExample();
        FailureInstanceExample failureInstanceExample = new FailureInstanceExample();
        if (StringUtils.isEmpty(appInstanceId)) {
            riskInstanceExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(now);
            alertInstanceExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(now);
            incidentInstanceExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(now);
            failureInstanceExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(now);
        } else {
            riskInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId).andGmtCreateGreaterThanOrEqualTo(now);
            alertInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId).andGmtCreateGreaterThanOrEqualTo(now);
            incidentInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId).andGmtCreateGreaterThanOrEqualTo(now);
            failureInstanceExample.createCriteria().andAppInstanceIdEqualTo(appInstanceId).andGmtCreateGreaterThanOrEqualTo(now);
        }

        long riskInsCnt = riskInstanceMapper.countByExample(riskInstanceExample);
        long alertInsCnt = alertInstanceMapper.countByExample(alertInstanceExample);
        long incidentInsCnt = incidentInstanceMapper.countByExample(incidentInstanceExample);
        long failureInsCnt = failureInstanceMapper.countByExample(failureInstanceExample);

        JSONObject result = new JSONObject();
        result.put(Constant.RISK, riskInsCnt);
        result.put(Constant.ALERT, alertInsCnt);
        result.put(Constant.INCIDENT, incidentInsCnt);
        result.put(Constant.FAILURE, failureInsCnt);
        return result;
    }

    @Override
    public JSONObject getDefinitionById(Integer id) {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(id);
        return convertToJSONObject(definition);
    }

    @Override
    public List<JSONObject> getDefinitionByIds(List<Integer> ids) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andIdIn(ids);
        List<CommonDefinition> definitions = definitionMapper.selectByExample(example);
        return convertToJSONObjects(definitions);
    }

    @Override
    public List<JSONObject> getDefinitionsByCategory(String category) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        if (StringUtils.isNotEmpty(category)) {
            example.createCriteria().andCategoryEqualTo(category);
        }
        return convertToJSONObjects(definitionMapper.selectByExample(example));
    }

    @Override
    public List<JSONObject> getDefinitionsByApp(String appId) {
        return getDefinitionsByAppComponent(appId, null);
    }

    @Override
    public List<JSONObject> getDefinitionsByAppComponent(String appId, String appComponentName) {
        return getDefinitions(appId, appComponentName, null, null);
    }

    @Override
    public List<JSONObject> getDefinitions(String appId, String appComponentName, String name, String category) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        CommonDefinitionExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appId)) {
            criteria.andAppIdEqualTo(appId);
        }
        if (StringUtils.isNotEmpty(appComponentName)) {
            criteria.andAppComponentNameEqualTo(appComponentName);
        }
        if (StringUtils.isNotEmpty(name)) {
            criteria.andNameEqualTo(name);
        }
        if (StringUtils.isNotEmpty(category)) {
            criteria.andCategoryEqualTo(category);
        }

        return convertToJSONObjects(definitionMapper.selectByExample(example));
    }

    @Override
    public boolean existDefinition(Integer id) {
        return definitionMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistDefinition(Integer id) {
        return !existDefinition(id);
    }

    @Override
    public int addDefinition(DefinitionCreateReq req) throws Exception {
        List<JSONObject> existDefinitions = getDefinitions(req.getAppId(), req.getAppComponentName(), req.getName(), req.getCategory());
        if (!CollectionUtils.isEmpty(existDefinitions)) {
            Optional<JSONObject> optional = existDefinitions.stream().filter(definition -> {
                String appComponentName = definition.getString("appComponentName");
                if (StringUtils.isNotBlank(appComponentName)) {
                    return appComponentName.equals(req.getAppComponentName());
                } else {
                    return StringUtils.isBlank(req.getAppComponentName());
                }
            }).findAny();
            if (optional.isPresent()) {
                throw new CommonDefinitionExistException(String.format("健康定义[name:%s]已经存在",req.getName()));
            }
        }

        // 每个指标ID只允许配置一条阈值告警规则
        if (req.getCategory().equals(Constant.ALERT)) {
            Integer metricId = req.getExConfig().getMetricId();
            if (metricId == null) {
                throw new ParamException("告警定义需要关联指标");
            }
            if (existMetricAlert(metricId)) {
                throw new CommonDefinitionExistException(String.format("指标[metric_id:%s]已经关联告警定义",metricId));
            }
        }

        // 每个异常定义ID只允许关联一条故障定义
        if (req.getCategory().equals(Constant.FAILURE) && existIncidentFailure(req.getExConfig().getRefIncidentDefId())) {
            throw new CommonDefinitionExistException(String.format("异常定义[def_id:%s]已经关联故障定义", req.getExConfig().getRefIncidentDefId()));
        }

        CommonDefinition commonDefinition = buildCommonDefinition(req);
        commonDefinition.setLastModifier(commonDefinition.getCreator());
        definitionMapper.insert(commonDefinition);

        if (req.getCategory().equals(Constant.INCIDENT)) {
            domainCacheService.expireKey(Constant.CACHE_INCIDENT_DEF_KEY);
        }

        return commonDefinition.getId();
    }

    @Override
    public int updateDefinition(DefinitionUpdateReq req) throws Exception {
        CommonDefinition existDefinition = definitionMapper.selectByPrimaryKey(req.getId());
        if (existDefinition == null) {
            throw new CommonDefinitionNotExistException(String.format("更新的定义[id:%s]不存在", req.getId()));
        }

        String category = StringUtils.isEmpty(req.getCategory()) ? existDefinition.getCategory() : req.getCategory();
        req.setCategory(category);

        // 每个指标ID只允许配置一条阈值告警规则
        if (category.equals(Constant.ALERT) && req.getExConfig() != null) {
            Integer newMetricId = req.getExConfig().getMetricId();
            Integer existMetricId = existDefinition.getMetricId();
            newMetricId = newMetricId == null ? existMetricId : newMetricId;
            if (!existMetricId.equals(newMetricId) && existMetricAlert(newMetricId)) {
                throw new CommonDefinitionExistException(String.format("指标[metric_id:%s]已经关联告警定义", newMetricId));
            }
            req.getExConfig().setMetricId(newMetricId);
        }

        // 每个异常定义ID只允许关联一条故障定义
        if (req.getCategory().equals(Constant.FAILURE) && existIncidentFailure(req.getExConfig().getRefIncidentDefId())) {
            throw new CommonDefinitionExistException(String.format("异常定义[def_id:%s]已经关联故障定义", req.getExConfig().getRefIncidentDefId()));
        }

        DefinitionExConfigReq exConfigReq = req.getExConfig();
        if (exConfigReq == null) {
            // 使用原来的
            exConfigReq = new DefinitionExConfigReq();
            DefinitionExConfigReq existExConfig = JSONObject.parseObject(existDefinition.getExConfig(), DefinitionExConfigReq.class);
            if (category.equals(Constant.INCIDENT)) {
                exConfigReq.setTypeId(existExConfig.getTypeId());
                exConfigReq.setSelfHealing(existExConfig.getSelfHealing());
                exConfigReq.setWeight(existExConfig.getWeight());
            } else if (category.equals(Constant.FAILURE)) {
                exConfigReq.setRefIncidentDefId(existExConfig.getRefIncidentDefId());
                exConfigReq.setFailureLevelRule(exConfigReq.getFailureLevelRule());
            } else if (category.equals(Constant.ALERT)) {
                exConfigReq.setEnable(existExConfig.getEnable());
                exConfigReq.setStorageDays(existExConfig.getStorageDays());
                exConfigReq.setGranularity(existExConfig.getGranularity());
                exConfigReq.setAlertRuleConfig(existExConfig.getAlertRuleConfig());
                exConfigReq.setNoticeConfig(existExConfig.getNoticeConfig());
                exConfigReq.setWeight(existExConfig.getWeight());
                exConfigReq.setMetricId(existExConfig.getMetricId());
            } else if (category.equals(Constant.RISK)) {
                exConfigReq.setEnable(existExConfig.getEnable());
                exConfigReq.setStorageDays(existExConfig.getStorageDays());
                exConfigReq.setTags(existExConfig.getTags());
                exConfigReq.setWeight(existExConfig.getWeight());
            } else if (category.equals(Constant.EVENT)) {
                exConfigReq.setType(existExConfig.getType());
                exConfigReq.setStorageDays(existExConfig.getStorageDays());
            }
        }

        CommonDefinition commonDefinition = buildCommonDefinition(req);
        commonDefinition.setId(req.getId());
        commonDefinition.setGmtCreate(null);

        int result = definitionMapper.updateByPrimaryKeySelective(commonDefinition);

        if (category.equals(Constant.INCIDENT)) {
            domainCacheService.expireKey(Constant.CACHE_INCIDENT_DEF_KEY);
        }

        return result;
    }

    private boolean existMetricAlert(Integer metricId) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andCategoryEqualTo(Constant.ALERT).andMetricIdEqualTo(metricId);
        List<CommonDefinition> definitions = definitionMapper.selectByExample(example);
        return !CollectionUtils.isEmpty(definitions);
    }

    private boolean existIncidentFailure(Integer incidentDefId) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andCategoryEqualTo(Constant.FAILURE);
        List<CommonDefinition> definitions = definitionMapper.selectByExample(example);
        Optional<CommonDefinition> any = definitions.stream().filter(definition ->
                JSONObject.parseObject(definition.getExConfig(), FailureExConfig.class).
                        getRefIncidentDefId().equals(incidentDefId)).findAny();
        return any.isPresent();
    }

    @Override
    @Transactional
    public int deleteDefinition(Integer id) throws Exception {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(id);
        if (definition == null) {
            return 0;
        }

        if (definition.getCategory().equals(Constant.INCIDENT)) {
            CommonDefinitionExample failureDefExample = new CommonDefinitionExample();
            failureDefExample.createCriteria().andCategoryEqualTo(Constant.FAILURE);
            List<CommonDefinition> failureDefinitions = definitionMapper.selectByExample(failureDefExample);
            if (failureDefinitions != null && !failureDefinitions.isEmpty()) {
                Optional<CommonDefinition> optDefinition = failureDefinitions.stream().filter(item -> {
                    FailureExConfig exConfig = JSONObject.parseObject(item.getExConfig(), FailureExConfig.class);
                    return exConfig.getRefIncidentDefId().equals(id);
                }).findAny();
                if (optDefinition.isPresent()) {
                    CommonDefinition usedDefinition = optDefinition.get();
                    throw new CommonDefinitionDeleteException(String.format("异常定义[id:%s]删除失败, 被故障定义[id:%s, name:%s]引用", id, usedDefinition.getId(), usedDefinition.getName()));
                }
            }

            IncidentInstanceExample example = new IncidentInstanceExample();
            example.createCriteria().andDefIdEqualTo(id);
            incidentInstanceMapper.deleteByExample(example);

        } else if (definition.getCategory().equals(Constant.FAILURE)) {
            FailureInstanceExample example = new FailureInstanceExample();
            example.createCriteria().andDefIdEqualTo(id);
            failureInstanceMapper.deleteByExample(example);
        } else if (definition.getCategory().equals(Constant.ALERT)) {
            AlertInstanceExample example = new AlertInstanceExample();
            example.createCriteria().andDefIdEqualTo(id);
            alertInstanceMapper.deleteByExample(example);
        } else if (definition.getCategory().equals(Constant.RISK)) {
            RiskInstanceExample example = new RiskInstanceExample();
            example.createCriteria().andDefIdEqualTo(id);
            riskInstanceMapper.deleteByExample(example);
        } else if (definition.getCategory().equals(Constant.EVENT)) {
            EventInstanceExample example = new EventInstanceExample();
            example.createCriteria().andDefIdEqualTo(id);
            eventInstanceMapper.deleteByExample(example);
        }

        int result = definitionMapper.deleteByPrimaryKey(id);

        if (definition.getCategory().equals(Constant.INCIDENT)) {
            domainCacheService.expireKey(Constant.CACHE_INCIDENT_DEF_KEY);
        }

        return result;
    }

    private CommonDefinition buildCommonDefinition(DefinitionBaseReq req) throws Exception {
        String category = req.getCategory();
        DefinitionExConfigReq exConfigReq = req.getExConfig();
        if (req.getExConfig() == null) {
            throw new ParamException("异常扩展配置缺失");
        }

        DefinitionExConfig definitionExConfig = null;
        // 扩展配置处理
        if (category.equals(Constant.INCIDENT)) {
            Integer typeId = exConfigReq.getTypeId();
            if (incidentTypeMapper.selectByPrimaryKey(typeId) == null) {
                throw new IncidentTypeNotExistException(String.format("异常定义失败, 关联异常类型[id:%s]不存在", typeId));
            }
            if (exConfigReq.getSelfHealing() == null) {
                exConfigReq.setSelfHealing(false);
            }
            if (exConfigReq.getWeight() == null) {
                exConfigReq.setWeight(0);
            }
            definitionExConfig = JSONObject.parseObject(JSONObject.toJSONString(exConfigReq), IncidentExConfig.class);
            DefExConfigValidator.validateIncidentExConfig((IncidentExConfig)definitionExConfig);
        } else if (category.equals(Constant.ALERT)) {
            if (exConfigReq.getStorageDays() == null) {
                exConfigReq.setStorageDays(Constant.DEFAULT_STORAGE_DAYS);
            }
            if (exConfigReq.getEnable() == null) {
                exConfigReq.setEnable(true);
            }
            if (exConfigReq.getWeight() == null) {
                exConfigReq.setWeight(0);
            }
            definitionExConfig = JSONObject.parseObject(JSONObject.toJSONString(exConfigReq), AlertExConfig.class);
            AlertExConfig alertExConfig = (AlertExConfig) definitionExConfig;
            DefExConfigValidator.validateAlertExConfig(alertExConfig);
        } else if (category.equals(Constant.FAILURE)) {
            definitionExConfig = JSONObject.parseObject(JSONObject.toJSONString(exConfigReq), FailureExConfig.class);
            DefExConfigValidator.validateFailureExConfig((FailureExConfig)definitionExConfig);
            exConfigReq.setRefIncidentDefId(((FailureExConfig) definitionExConfig).getRefIncidentDefId());
        } else if (category.equals(Constant.RISK)) {
//            if (riskTypeMapper.selectByPrimaryKey(typeId) == null) {
//                throw new RiskTypeNotExistException(String.format("风险定义失败, 关联风险类型[id:%s]不存在", typeId));
//            }
            if (exConfigReq.getStorageDays() == null) {
                exConfigReq.setStorageDays(Constant.DEFAULT_STORAGE_DAYS);
            }
            if (exConfigReq.getEnable() == null) {
                exConfigReq.setEnable(true);
            }
            if (exConfigReq.getWeight() == null) {
                exConfigReq.setWeight(0);
            }
            definitionExConfig = JSONObject.parseObject(JSONObject.toJSONString(exConfigReq), RiskExConfig.class);
            DefExConfigValidator.validateRiskExConfig((RiskExConfig) definitionExConfig);
        }  else if (category.equals(Constant.EVENT)) {
            if (exConfigReq.getStorageDays() == null) {
                exConfigReq.setStorageDays(Constant.DEFAULT_STORAGE_DAYS);
            }
            definitionExConfig = JSONObject.parseObject(JSONObject.toJSONString(exConfigReq), EventExConfig.class);
            DefExConfigValidator.validateEventExConfig((EventExConfig) definitionExConfig);
        }

        CommonDefinition definition = new CommonDefinition();
        Date now = new Date();
        definition.setGmtCreate(now);
        definition.setGmtModified(now);
        definition.setName(req.getName());
        definition.setCategory(req.getCategory());
        definition.setAppId(req.getAppId());
        definition.setAppName(req.getAppName());
        definition.setAppComponentName(req.getAppComponentName());
        definition.setMetricId(req.getExConfig().getMetricId());
        definition.setFailureRefIncidentId(req.getExConfig().getRefIncidentDefId());
        definition.setCreator(req.getCreator());
        definition.setReceivers(req.getReceivers());
        definition.setLastModifier(req.getLastModifier());
        definition.setExConfig(definitionExConfig == null ? null : JSONObject.toJSONString(definitionExConfig));
        definition.setDescription(req.getDescription());

        return definition;
    }

    @Override
    public JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }

        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(obj));

        String exConfigStr = result.getString("exConfig");
        if (StringUtils.isNotEmpty(exConfigStr)) {
            String category = result.getString("category");
            if (category.equals(Constant.INCIDENT)) {
                IncidentExConfig exConfig = JSONObject.parseObject(exConfigStr, IncidentExConfig.class);
                result.put("exConfig", exConfig);
                result.putAll(exConfig.buildRet());

                IncidentTypeCache incidentTypeCache = (IncidentTypeCache)domainCacheService.getDomainCache(Constant.CACHE_INCIDENT_TYPE_KEY);
                result.put("incidentType", incidentTypeCache.getIncidentTypeMap().getOrDefault(exConfig.getTypeId(), null));
                result.put("typeName", incidentTypeCache.getIncidentTypeMap().getOrDefault(exConfig.getTypeId(), null).getName());
            } else if (category.equals(Constant.ALERT)) {
                AlertExConfig exConfig = JSONObject.parseObject(exConfigStr, AlertExConfig.class);
                result.put("exConfig", exConfig);
                result.putAll(exConfig.buildRet());
            } else if (category.equals(Constant.RISK)) {
                RiskExConfig exConfig = JSONObject.parseObject(exConfigStr, RiskExConfig.class);
                result.put("exConfig", exConfig);
                result.putAll(exConfig.buildRet());

//                RiskTypeCache riskTypeCache = (RiskTypeCache)domainCacheService.getDomainCache(Constant.CACHE_RISK_TYPE_KEY);
//                result.put("riskType", riskTypeCache.getRiskTypeMap().getOrDefault(exConfig.getTypeId(), null));
//                result.put("typeName", riskTypeCache.getRiskTypeMap().getOrDefault(exConfig.getTypeId(), null).getName());
            }  else if (category.equals(Constant.FAILURE)) {
                FailureExConfig exConfig = JSONObject.parseObject(exConfigStr, FailureExConfig.class);
                result.put("exConfig", exConfig);
                result.putAll(exConfig.buildRet());

                IncidentDefCache incidentDefCache = (IncidentDefCache)domainCacheService.getDomainCache(Constant.CACHE_INCIDENT_DEF_KEY);
                result.put("refIncidentDefName", incidentDefCache.getCommonDefinitionMap().getOrDefault(exConfig.getRefIncidentDefId(), null).getName());
            } else if (category.equals(Constant.EVENT)) {
                EventExConfig exConfig = JSONObject.parseObject(exConfigStr, EventExConfig.class);
                result.put("exConfig", exConfig);
                result.putAll(exConfig.buildRet());
            }
        }

        return result;
    }
}
