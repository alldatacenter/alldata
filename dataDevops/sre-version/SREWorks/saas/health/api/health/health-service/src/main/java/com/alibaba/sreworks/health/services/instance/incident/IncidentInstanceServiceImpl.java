package com.alibaba.sreworks.health.services.instance.incident;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.incident.IncidentInstanceService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionNotExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.common.incident.SelfHealingStatus;
import com.alibaba.sreworks.health.common.utils.IDGenerator;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.bo.IncidentExConfig;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceHealingReq;
import com.alibaba.sreworks.health.producer.HealthKafkaProducer;
import com.alibaba.sreworks.health.producer.IncidentInstanceRecord;
import com.alibaba.sreworks.health.services.instance.InstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 异常事件实例Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 14:51
 */
@Slf4j
@Service
public class IncidentInstanceServiceImpl extends InstanceService implements IncidentInstanceService {

    @Autowired
    IncidentInstanceMapper incidentInstanceMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    HealthKafkaProducer healthKafkaProducer;

    @Override
    public List<JSONObject> getIncidentsTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId) {
        // TODO 按照日期缓存统计数据
        Map<String, Object> params = buildTimeAggParams(timeUnit, startTimestamp, endTimestamp, appInstanceId);
        List<JSONObject> results = incidentInstanceMapper.countGroupByTime(params);
        return convertToJSONObjects(richTimeAgg(timeUnit, startTimestamp, endTimestamp, results));
    }

    @Override
    public JSONObject getIncidentById(Long id) {
        IncidentInstance incidentInstance = incidentInstanceMapper.selectByPrimaryKey(id);
        JSONObject result = convertToJSONObject(incidentInstance);
        return richInstance(result);
    }

    @Override
    public List<JSONObject> getIncidentsByTrace(String traceId) {
        IncidentInstanceExample example = new IncidentInstanceExample();
        example.createCriteria().andTraceIdEqualTo(traceId);

        List<JSONObject> results = convertToJSONObjects(incidentInstanceMapper.selectByExampleWithBLOBs(example));
        return richInstances(results);
    }

    @Override
    public List<JSONObject> getIncidentsByApp(String appId) {
        return getIncidents(appId, null, null, null, null, null, null);
    }

    @Override
    public List<JSONObject> getIncidents(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Integer incidentTypeId, Long startTimestamp, Long endTimestamp) {
        List<JSONObject> results = convertToJSONObjects(doGetIncidents(appId, appInstanceId, appComponentName, appComponentInstanceId, incidentTypeId, startTimestamp, endTimestamp, false));
        return richInstances(results);
    }

    @Override
    public List<JSONObject> getIncidentsGroupByTrace(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Integer incidentTypeId, String traceId, Long startTimestamp, Long endTimestamp) {
        List<IncidentInstance> incidentInstances = doGetIncidents(appId, appInstanceId, appComponentName, appComponentInstanceId, incidentTypeId, startTimestamp, endTimestamp, true);
        if (CollectionUtils.isEmpty(incidentInstances)) {
            return convertToJSONObjects(null);
        }

        if (StringUtils.isNotEmpty(traceId)) {
            incidentInstances = incidentInstances.stream().filter(incidentInstance -> incidentInstance.getTraceId().equals(traceId)).collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(incidentInstances)) {
            return convertToJSONObjects(null);
        }

        Map<String, List<IncidentInstance>> instancesTraceMap = new HashMap<>();
        incidentInstances.forEach(incidentInstance -> {
            String keyTraceId = incidentInstance.getTraceId();
            if (instancesTraceMap.containsKey(keyTraceId)) {
                instancesTraceMap.get(keyTraceId).add(incidentInstance);
            } else {
                List<IncidentInstance> ele = new ArrayList<>();
                ele.add(incidentInstance);
                instancesTraceMap.put(keyTraceId, ele);
            }
        });

        List<JSONObject> result = new ArrayList<>();
        for (String keyTraceId : instancesTraceMap.keySet()) {
            List<IncidentInstance> instances =  instancesTraceMap.get(keyTraceId);
            Collections.sort(instances, (o1, o2) -> {
                int o1Id = Integer.parseInt(o1.getSpanId());
                int o2Id = Integer.parseInt(o2.getSpanId());
                if (o1Id > o2Id) {
                    return 1;
                } else if (o1Id < o2Id) {
                    return -1;
                } else {
                    return 0;
                }
            });

            JSONObject parentInstance = convertToJSONObject(instances.remove(0));
            parentInstance.put("subIns", convertToJSONObjects(instances));
            result.add(parentInstance);
        }
        return result;
    }

    private List<IncidentInstance> doGetIncidents(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Integer incidentTypeId, Long startTimestamp, Long endTimestamp, boolean selfHealing) {
        List<CommonDefinition> definitions = getDefs(appId, appComponentName, Constant.INCIDENT);
        if (CollectionUtils.isEmpty(definitions)) {
            return null;
        }

        List<Integer> defIds = definitions.stream()
                .filter(definition -> incidentTypeId == null || JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class).getTypeId().equals(incidentTypeId))
                .map(CommonDefinition::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(defIds)) {
            return null;
        }

        IncidentInstanceExample example = new IncidentInstanceExample();
        IncidentInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (StringUtils.isNotEmpty(appComponentInstanceId)) {
            criteria.andAppComponentInstanceIdEqualTo(appComponentInstanceId);
        }
        criteria.andDefIdIn(defIds);
        if (startTimestamp != null) {
            criteria.andGmtLastOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtLastOccurLessThan(new Date(endTimestamp));
        }
        if (selfHealing) {
            criteria.andTraceIdIsNotNull();
        }

        return  incidentInstanceMapper.selectByExampleWithBLOBs(example);
    }

    private IncidentInstance getAliveIncident(Integer defId, String appInstanceId, String appComponentInstanceId) throws Exception {
        IncidentInstanceExample example = new IncidentInstanceExample();
        if (StringUtils.isEmpty(appComponentInstanceId)) {
            example.createCriteria().andDefIdEqualTo(defId).andAppInstanceIdEqualTo(appInstanceId).
                    andGmtRecoveryIsNull();
        } else {
            example.createCriteria().andDefIdEqualTo(defId).andAppInstanceIdEqualTo(appInstanceId).
                    andAppComponentInstanceIdEqualTo(appComponentInstanceId).andGmtRecoveryIsNull();
        }

        List<IncidentInstance> instances = incidentInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        } else {
            if (instances.size() > 1) {
                throw new ParamException(String.format("待恢复异常实例不唯一,定义ID:%s,应用实例:%s,应用组件实例:%s", defId, appInstanceId, appComponentInstanceId));
            }
            return instances.get(0);
        }
    }

    @Override
    public boolean existIncident(Long id) {
        return incidentInstanceMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistIncident(Long id) {
        return !existIncident(id);
    }

    @Override
    public JSONObject pushIncident(Integer defId, JSONObject incident) throws Exception {
        IncidentInstanceCreateReq req = JSONObject.toJavaObject(incident, IncidentInstanceCreateReq.class);
        req.setDefId(defId);
        // 确认是否已经存在异常实例(恢复时间为空的实例)
        IncidentInstance aliveIncident = getAliveIncident(req.getDefId(), req.getAppInstanceId(), req.getAppComponentInstanceId());
        if (aliveIncident == null) {
            return addIncident(req);
        } else {
            aliveIncident.setGmtLastOccur(new Timestamp(req.getOccurTs()));
            aliveIncident.setOccurTimes(aliveIncident.getOccurTimes() + 1);
            aliveIncident.setSource(req.getSource());
            aliveIncident.setCause(req.getCause());
            aliveIncident.setOptions(req.getOptions());
            aliveIncident.setDescription(req.getDescription());

            CommonDefinition definition = definitionMapper.selectByPrimaryKey(req.getDefId());
            IncidentExConfig incidentExConfig = JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class);
            if (incidentExConfig.getSelfHealing()) {
                aliveIncident.setSelfHealingStatus(SelfHealingStatus.WAITING.name());
            }

            return updateAliveIncident(aliveIncident);
        }
    }

    @Override
    @Transactional
    public JSONObject addIncident(IncidentInstanceCreateReq req) throws Exception {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(req.getDefId());
        if (definition == null) {
            throw new CommonDefinitionNotExistException(String.format("异常定义[id:%s]不存在", req.getDefId()));
        }
        IncidentExConfig incidentExConfig = JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class);
        if (incidentExConfig.getSelfHealing()) {
            if (StringUtils.isEmpty(req.getTraceId())) {
                req.setTraceId(buildTraceId(req.getDefId(), req.getAppInstanceId(), req.getAppComponentInstanceId(), req.getOccurTs()));
                req.setSpanId("0");
            }
            if (StringUtils.isEmpty(req.getSpanId())) {
                req.setSpanId("0");
            }
        }

        IncidentInstance incident = buildIncidentInstance(req);
        incidentInstanceMapper.insert(incident);
        pushKafkaMsg(incident, incidentExConfig.getSelfHealing());

        JSONObject result = new JSONObject();
        result.put("incident_id", incident.getId());
        result.put("trace_id", incident.getTraceId());
        return result;
    }

    @Override
    public JSONObject updateIncidentSelfHealing(Integer defId, String appInstanceId, String appComponentInstanceId, IncidentInstanceHealingReq req) throws Exception {
        IncidentInstance aliveIncident = getAliveIncident(defId, appInstanceId, appComponentInstanceId);
        if (aliveIncident != null) {
            return updateIncidentSelfHealing(aliveIncident.getId(), req);
        } else {
            throw new ParamException(String.format("异常实例[def:%s, appInstanceId:%s, appComponentInstanceId:%s]不存在或已经恢复",defId, appInstanceId, appComponentInstanceId));
        }
    }

    @Override
    public List<JSONObject> updateIncidentSelfHealing(String traceId, IncidentInstanceHealingReq req) throws Exception {
        IncidentInstanceExample example = new IncidentInstanceExample();
        example.createCriteria().andTraceIdEqualTo(traceId).andGmtRecoveryIsNull();
        List<IncidentInstance> instances = incidentInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        } else {
            List<JSONObject> results = new ArrayList<>();
            for (IncidentInstance instance : instances) {
                results.add(updateIncidentSelfHealing(instance.getId(), req));
            }
            return results;
        }
    }

    @Override
    public JSONObject updateIncidentSelfHealing(Long id, IncidentInstanceHealingReq req) throws Exception {
        IncidentInstance aliveIncident = incidentInstanceMapper.selectByPrimaryKey(id);
        if (aliveIncident != null && aliveIncident.getGmtRecovery() == null) {
            aliveIncident.setGmtSelfHealingStart(req.getSelfHealingStartTs() != null ? new Timestamp(req.getSelfHealingStartTs()) : new Date());
            aliveIncident.setGmtSelfHealingEnd(req.getSelfHealingEndTs() != null ? new Timestamp(req.getSelfHealingEndTs()) : new Date());
            String selfHealingStatus = req.getSelfHealingStatus();
            aliveIncident.setSelfHealingStatus(selfHealingStatus);
            if (selfHealingStatus != null && SelfHealingStatus.valueOf(selfHealingStatus) == SelfHealingStatus.SUCCESS) {
                aliveIncident.setGmtRecovery(new Timestamp(System.currentTimeMillis()));
            }

            incidentInstanceMapper.updateByPrimaryKeySelective(aliveIncident);

            IncidentInstance incidentInstance = incidentInstanceMapper.selectByPrimaryKey(id);
            pushKafkaMsg(incidentInstance, true);

            JSONObject result = new JSONObject();
            result.put("incident_id", incidentInstance.getId());
            result.put("trace_id", incidentInstance.getTraceId());
            return result;
        } else {
            throw new ParamException(String.format("异常实例[id:%s]不存在或已经恢复", id));
        }
    }

    @Override
    public boolean recoveryIncident(Integer defId, String appInstanceId, String appComponentInstanceId) throws Exception {
        IncidentInstance aliveIncident = getAliveIncident(defId, appInstanceId, appComponentInstanceId);
        if (aliveIncident != null) {
            return recoveryIncident(aliveIncident.getId());
        }
        return true;
    }

    @Override
    public boolean recoveryIncident(Long id) {
        IncidentInstance recoveredIncident = new IncidentInstance();
        recoveredIncident.setId(id);
        recoveredIncident.setGmtRecovery(new Timestamp(System.currentTimeMillis()));
        incidentInstanceMapper.updateByPrimaryKeySelective(recoveredIncident);

        IncidentInstance incidentInstance = incidentInstanceMapper.selectByPrimaryKey(id);

        CommonDefinition definition = definitionMapper.selectByPrimaryKey(incidentInstance.getDefId());
        IncidentExConfig incidentExConfig = JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class);
        pushKafkaMsg(incidentInstance, incidentExConfig.getSelfHealing());

        return true;
    }

    @Transactional
    JSONObject updateAliveIncident(IncidentInstance aliveIncident) throws Exception {
        CommonDefinition definition = definitionMapper.selectByPrimaryKey(aliveIncident.getDefId());
        if (definition == null) {
            throw new CommonDefinitionNotExistException(String.format("异常定义[id:%s]不存在", aliveIncident.getDefId()));
        }
        IncidentExConfig incidentExConfig = JSONObject.parseObject(definition.getExConfig(), IncidentExConfig.class);
        if (StringUtils.isEmpty(aliveIncident.getTraceId()) && incidentExConfig.getSelfHealing()) {
            aliveIncident.setTraceId(buildTraceId(aliveIncident.getDefId(), aliveIncident.getAppInstanceId(), aliveIncident.getAppComponentInstanceId(), aliveIncident.getGmtOccur().getTime()));
            aliveIncident.setSpanId("0");
        }
        incidentInstanceMapper.updateByPrimaryKeySelective(aliveIncident);

//        if (incidentExConfig.getSelfHealing()) {
//            pushKafkaMsg(aliveIncident);
//        }
        pushKafkaMsg(aliveIncident, incidentExConfig.getSelfHealing());

        JSONObject result = new JSONObject();
        result.put("incident_id", aliveIncident.getId());
        result.put("trace_id", aliveIncident.getTraceId());
        return result;
    }

    private void pushKafkaMsg(IncidentInstance incident, boolean isSelfHealing) {
        incident.setAppInstanceId(StringUtils.isEmpty(incident.getAppInstanceId()) ? "-1" : incident.getAppInstanceId());
        incident.setAppComponentInstanceId(StringUtils.isEmpty(incident.getAppComponentInstanceId()) ? "-1" : incident.getAppComponentInstanceId());

        IncidentInstanceRecord record = new IncidentInstanceRecord();
        // 异常未恢复 实例推送自愈通道
        if (isSelfHealing && incident.getGmtRecovery() == null) {
            // 未自愈 或者 非RUNNING和CANCEL状态自愈实例
            if (StringUtils.isEmpty(incident.getSelfHealingStatus()) ||
                    (!incident.getSelfHealingStatus().equals(SelfHealingStatus.RUNNING.name()) && !incident.getSelfHealingStatus().equals(SelfHealingStatus.CANCEL.name()))) {
                healthKafkaProducer.sendMsg(record.buildProducerRecord("sreworks-health-incident-" + incident.getDefId(), incident));
            }
        }

        incident.setGmtRecovery(incident.getGmtRecovery() == null ? new Timestamp(0) : incident.getGmtRecovery());
        healthKafkaProducer.sendMsg(record.buildProducerRecord("sreworks-health-incident", incident));
    }

    private String buildTraceId(Integer defId, String appInstanceId, String appComponentInstanceId, Long timestamp) {
        return IDGenerator.generateInstanceId(
                String.format("def_id:%s,app_instance_id:%s,app_component_instance_id:%s,occur_ts:%s", defId, appInstanceId,
                        appComponentInstanceId, timestamp)
        );
    }

    @Override
    public int deleteIncident(Long id) throws Exception {
        return incidentInstanceMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int deleteIncidentsByTrace(String traceId) throws Exception {
        IncidentInstanceExample example = new IncidentInstanceExample();
        example.createCriteria().andTraceIdEqualTo(traceId);

        List<IncidentInstance> instances = incidentInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return 0;
        }

        return incidentInstanceMapper.deleteByExample(example);
    }

    private IncidentInstance buildIncidentInstance(IncidentInstanceCreateReq req) {
        IncidentInstance incidentInstance = new IncidentInstance();
        Date now = new Date();
        incidentInstance.setGmtCreate(now);
        incidentInstance.setGmtModified(now);
        incidentInstance.setDefId(req.getDefId());
        incidentInstance.setAppInstanceId(req.getAppInstanceId());
        incidentInstance.setAppComponentInstanceId(req.getAppComponentInstanceId());
        incidentInstance.setGmtOccur(new Timestamp(req.getOccurTs()));
        incidentInstance.setGmtLastOccur(new Timestamp(req.getOccurTs()));
        incidentInstance.setOccurTimes(1);
        incidentInstance.setGmtRecovery(req.getRecoveryTs() != null ? new Timestamp(req.getRecoveryTs()) : null);
        incidentInstance.setSource(req.getSource());
        incidentInstance.setOptions(req.getOptions());
        incidentInstance.setCause(req.getCause());
        incidentInstance.setDescription(req.getDescription());
        incidentInstance.setTraceId(req.getTraceId());
        incidentInstance.setSpanId(req.getSpanId());
        incidentInstance.setGmtSelfHealingStart(req.getSelfHealingStartTs() != null ? new Timestamp(req.getSelfHealingStartTs()) : null);
        incidentInstance.setGmtSelfHealingEnd(req.getSelfHealingEndTs() != null ? new Timestamp(req.getSelfHealingEndTs()) : null);
        incidentInstance.setSelfHealingStatus(req.getSelfHealingStatus());

        return incidentInstance;
    }
}
