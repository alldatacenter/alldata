package com.alibaba.sreworks.health.services.instance.alert;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.alert.AlertInstanceService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionNotExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.req.alert.AlertInstanceCreateReq;
import com.alibaba.sreworks.health.producer.AlertInstanceRecord;
import com.alibaba.sreworks.health.producer.HealthKafkaProducer;
import com.alibaba.sreworks.health.services.instance.InstanceService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警服务类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 19:36
 */
@Service
@Slf4j
public class AlertInstanceServiceImpl extends InstanceService implements AlertInstanceService {

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    AlertInstanceMapper alertInstanceMapper;

    @Autowired
    HealthKafkaProducer healthKafkaProducer;

    @Override
    public List<JSONObject> getAlertsTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId) {
        // TODO 按照日期缓存统计数据
        Map<String, Object> params = buildTimeAggParams(timeUnit, startTimestamp, endTimestamp, appInstanceId);
        List<JSONObject> results = alertInstanceMapper.countGroupByTime(params);
        return convertToJSONObjects(richTimeAgg(timeUnit, startTimestamp, endTimestamp, results));
    }

    @Override
    public List<JSONObject> getAlertsByApp(String appId) {
        return getAlertsByInstance(appId, null);
    }

    @Override
    public List<JSONObject> getAlertsByInstance(String appId, String appInstanceId) {
        return getAlerts(appId, appInstanceId, null, null,null, null);
    }

    @Override
    public List<JSONObject> getAlerts(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp) {
        List<Integer> defIds = getDefIds(appId, appComponentName, Constant.ALERT);
        if (CollectionUtils.isEmpty(defIds)) {
            return convertToJSONObjects(null);
        }

        AlertInstanceExample example = new AlertInstanceExample();
        AlertInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId).andDefIdIn(defIds);
        }
        if (StringUtils.isNotEmpty(appComponentInstanceId)) {
            criteria.andAppComponentInstanceIdEqualTo(appComponentInstanceId);
        }
        criteria.andDefIdIn(defIds);
        if (startTimestamp != null) {
            criteria.andGmtOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtOccurLessThan(new Date(endTimestamp));
        }

        List<JSONObject> results = convertToJSONObjects(alertInstanceMapper.selectByExampleWithBLOBs(example));
        return richInstances(results);
    }

    @Override
    public JSONObject getAlertById(Long id) {
        AlertInstance alertInstance = alertInstanceMapper.selectByPrimaryKey(id);
        JSONObject result = convertToJSONObject(alertInstance);
        return richInstance(result);
    }

    @Override
    public boolean existAlert(Long id) {
        return alertInstanceMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistAlert(Long id) {
        return !existAlert(id);
    }

    @Override
    public int pushAlerts(Integer defId, List<JSONObject> alerts) throws Exception {
        if (CollectionUtils.isEmpty(alerts)) {
            return 0;
        } else if (alerts.size() > Constant.MAX_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_DATA_FLUSH_SIZE));
        }

        CommonDefinition definition = getRefDefinition(defId, Constant.ALERT);
        if (definition == null) {
            throw new CommonDefinitionNotExistException(String.format("告警定义[id:%s]不存在", defId));
        }

        List<AlertInstance> alertInstances = alerts.parallelStream().map(alert -> {
            AlertInstanceCreateReq req = JSONObject.toJavaObject(alert, AlertInstanceCreateReq.class);
            req.setDefId(defId);
            return buildAlertInstanceSimple(req);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(alertInstances)) {
            return 0;
        }

        List<List<AlertInstance>> alertInstancesList = Lists.partition(alertInstances, Constant.DATA_FLUSH_STEP);
        alertInstancesList.forEach(items -> alertInstanceMapper.batchInsert(items));

        for (AlertInstance alertInstance : alertInstances) {
            pushKafkaMsg(alertInstance);
        }

        return alertInstances.size();
    }

    @Override
    @Transactional
    public int addAlert(AlertInstanceCreateReq req) throws Exception {
        if (!existRefDefinition(req.getDefId(), Constant.ALERT)) {
            throw new CommonDefinitionNotExistException(String.format("告警定义[id:%s]不存在", req.getDefId()));
        }
        AlertInstance alertInstance = buildAlertInstanceSimple(req);
        int result = alertInstanceMapper.insert(alertInstance);
        pushKafkaMsg(alertInstance);
        return result;
    }

    @Override
    public int deleteAlert(Long id) throws Exception {
        return alertInstanceMapper.deleteByPrimaryKey(id);
    }

    private void pushKafkaMsg(AlertInstance alert) {
        alert.setAppInstanceId(StringUtils.isEmpty(alert.getAppInstanceId()) ? "-1" : alert.getAppInstanceId());
        alert.setAppComponentInstanceId(StringUtils.isEmpty(alert.getAppComponentInstanceId()) ? "-1" : alert.getAppComponentInstanceId());

        AlertInstanceRecord record = new AlertInstanceRecord();
        healthKafkaProducer.sendMsg(record.buildProducerRecord("sreworks-health-alert-" + alert.getDefId(), alert));
    }

    private AlertInstance buildAlertInstanceSimple(AlertInstanceCreateReq req) {
        AlertInstance alertInstance = new AlertInstance();
        Date now = new Date();
        alertInstance.setGmtCreate(now);
        alertInstance.setGmtModified(now);
        alertInstance.setDefId(req.getDefId());
        alertInstance.setAppInstanceId(req.getAppInstanceId());
        alertInstance.setAppComponentInstanceId(req.getAppComponentInstanceId());
        alertInstance.setMetricInstanceId(req.getMetricInstanceId());
        alertInstance.setMetricInstanceLabels(req.getMetricInstanceLabels().toJSONString());
        alertInstance.setGmtOccur(new Timestamp(req.getOccurTs()));
        alertInstance.setSource(req.getSource());
        alertInstance.setLevel(req.getLevel());
        alertInstance.setReceivers(req.getReceivers());
        alertInstance.setContent(req.getContent());

        return alertInstance;
    }
}
