package com.alibaba.sreworks.health.services.ocenter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 运维事件定义Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 15:55
 */
@Slf4j
@Service
public class OcenterService{

    private static final double MAX_HEALTH_SCORE = 100;

    private static final double MIN_HEALTH_SCORE = 0;

    private static final double RISK_RATIO = 0.05;

    private static final double ALERT_RATIO = 0.1;

    private static final double INCIDENT_RATIO = 1;

    private static final long ONE_MINUTE_MS = 60000;

    private static final String SLA_LABEL = "SERVICE_UNAVAILABLE";

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    RiskInstanceMapper riskInstanceMapper;

    @Autowired
    AlertInstanceMapper alertInstanceMapper;

    @Autowired
    IncidentInstanceMapper incidentInstanceMapper;

    @Autowired
    IncidentTypeMapper incidentTypeMapper;

    @Autowired
    FailureInstanceMapper failureInstanceMapper;

    public Double getHealthScore(String appInstanceId) {
        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR, -1);
        long startTimestamp = calendar.getTimeInMillis();
        long endTimestamp = now.getTime();

        List<Integer> defIds = new ArrayList<>();

        // 统计最近一小时的风险实例
        List<RiskInstance> riskInstances = getRiskIns(appInstanceId, startTimestamp, endTimestamp);
        riskInstances.forEach(riskInstance -> defIds.add(riskInstance.getDefId()));

        // 统计最近一小时的告警实例
        List<AlertInstance> alertInstances = getAlertIns(appInstanceId, startTimestamp, endTimestamp);
        alertInstances.forEach(alertInstance -> defIds.add(alertInstance.getDefId()));

        // 统计未恢复的异常
        List<IncidentInstance> unRecoveryIncidentInstances = getUnRecoveryIncidentIns(appInstanceId, endTimestamp);
        unRecoveryIncidentInstances.forEach(incidentInstance -> defIds.add(incidentInstance.getDefId()));

        if (CollectionUtils.isEmpty(defIds)) {
            return MAX_HEALTH_SCORE;
        }
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andIdIn(defIds);
        List<CommonDefinition> definitions = definitionMapper.selectByExample(example);
        Map<Integer, Integer> weightMap = definitions.stream().collect(Collectors.toMap(CommonDefinition::getId, definition -> {
            JSONObject exConfig = JSONObject.parseObject(definition.getExConfig());
            return exConfig.getInteger("weight");
        }));

        double riskScore = 0;
        for (RiskInstance riskInstance : riskInstances) {
            riskScore += weightMap.get(riskInstance.getDefId()) * RISK_RATIO;
        }

        double alertScore = 0;
        for (AlertInstance alertInstance : alertInstances) {
            alertScore += weightMap.get(alertInstance.getDefId()) * ALERT_RATIO;
        }

        double incidentScore = 0;
        for (IncidentInstance riskInstance : unRecoveryIncidentInstances) {
            incidentScore += weightMap.get(riskInstance.getDefId()) * INCIDENT_RATIO;
        }

        double healthScore = MAX_HEALTH_SCORE - riskScore - alertScore - incidentScore;
        if (healthScore < 0) {
            healthScore = MIN_HEALTH_SCORE;
        }
        return healthScore;
    }

    public JSONObject getSla(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        JSONObject appInstanceSla = new JSONObject();

        List<IncidentInstance> incidentInstances = getSlaIncidentIns(appInstanceId, startTimestamp, endTimestamp);

        IncidentTypeExample incidentTypeExample = new IncidentTypeExample();
        incidentTypeExample.createCriteria().andLabelEqualTo(SLA_LABEL);
        List<IncidentType> incidentTypes = incidentTypeMapper.selectByExample(incidentTypeExample);

        if (CollectionUtils.isEmpty(incidentTypes) || CollectionUtils.isEmpty(incidentInstances)) {
            incidentInstances.forEach(incidentInstance -> appInstanceSla.put(incidentInstance.getAppInstanceId(), 1.0));
            return appInstanceSla;
        }

        List<Integer> defIds = new ArrayList<>();
        incidentInstances.forEach(incidentInstance -> defIds.add(incidentInstance.getDefId()));
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andIdIn(defIds);
        List<CommonDefinition> definitions = definitionMapper.selectByExample(example);

        List<Integer> slaDefIds = definitions.stream().filter(definition -> {
            Integer typeId = JSONObject.parseObject(definition.getExConfig()).getInteger("type_id");
            return typeId.equals(incidentTypes.get(0).getId());
        }).map(CommonDefinition::getId).collect(Collectors.toList());

        Map<String, Set<Long>> appInstanceIncidentTimestamp = new HashMap<>();
        incidentInstances.forEach(incidentInstance -> {
            if (slaDefIds.contains(incidentInstance.getDefId())) {
                String appInsId = incidentInstance.getAppInstanceId();
                Set<Long> incidentTimestamp = new HashSet<>();
                if (appInstanceIncidentTimestamp.containsKey(appInsId)) {
                    incidentTimestamp = appInstanceIncidentTimestamp.get(appInsId);
                } else {
                    appInstanceIncidentTimestamp.put(appInsId, incidentTimestamp);
                }

                Long startTs = Math.max(incidentInstance.getGmtOccur().getTime(), startTimestamp);
                Long endTs = incidentInstance.getGmtRecovery() == null ? endTimestamp : Math.min(incidentInstance.getGmtRecovery().getTime(), endTimestamp);
                Long startMinuteTs = startTs - startTs % ONE_MINUTE_MS;
                Long endMinuteTs = endTs - endTs % ONE_MINUTE_MS;
                while (startMinuteTs <= endMinuteTs) {
                    incidentTimestamp.add(startMinuteTs);
                    startMinuteTs += ONE_MINUTE_MS;
                }
            } else {
                appInstanceIncidentTimestamp.put(incidentInstance.getAppInstanceId(), new HashSet<>());
            }
        });

        DecimalFormat df =new DecimalFormat("0.00000");
        appInstanceIncidentTimestamp.forEach((appInsId, incidentTimestamp) ->{
            Double sla = Double.valueOf(df.format(1 - ((double)incidentTimestamp.size() / (double)((endTimestamp - startTimestamp) / ONE_MINUTE_MS + 1))));
            appInstanceSla.put(appInsId, sla);
        });

        return appInstanceSla;
    }

    public JSONObject getInstanceCntStat(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        JSONObject result = new JSONObject();
        result.put(Constant.RISK, getRiskIns(appInstanceId, startTimestamp, endTimestamp).size());
        result.put(Constant.ALERT, getAlertIns(appInstanceId, startTimestamp, endTimestamp).size());
        result.put(Constant.INCIDENT, getIncidentIns(appInstanceId, startTimestamp, endTimestamp).size());
        result.put(Constant.FAILURE, getFailureIns(appInstanceId, startTimestamp, endTimestamp).size());

        return result;
    }

    private List<RiskInstance> getRiskIns(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        RiskInstanceExample example = new RiskInstanceExample();
        RiskInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (startTimestamp != null) {
            criteria.andGmtOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtOccurLessThan(new Date(endTimestamp));
        }

        return riskInstanceMapper.selectByExample(example);
    }

    private List<AlertInstance> getAlertIns(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        AlertInstanceExample example = new AlertInstanceExample();
        AlertInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (startTimestamp != null) {
            criteria.andGmtOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtOccurLessThan(new Date(endTimestamp));
        }

        return alertInstanceMapper.selectByExample(example);
    }

    private List<IncidentInstance> getUnRecoveryIncidentIns(String appInstanceId, Long endTimestamp) {
        IncidentInstanceExample example = new IncidentInstanceExample();

        // 统计未恢复实例
        IncidentInstanceExample.Criteria orCriteria = example.or();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            orCriteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (endTimestamp != null) {
            // 发生时间不晚于查询截止时间
            orCriteria.andGmtOccurLessThanOrEqualTo(new Date(endTimestamp));
        }
        orCriteria.andGmtRecoveryIsNull();

        return incidentInstanceMapper.selectByExample(example);
    }

    private List<IncidentInstance> getSlaIncidentIns(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        IncidentInstanceExample example = new IncidentInstanceExample();

        // 统计已经恢复实例(恢复时间晚于统计开始时间)
        IncidentInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        criteria.andGmtRecoveryIsNotNull().andGmtRecoveryGreaterThanOrEqualTo(new Date(startTimestamp));

        // 统计未恢复实例(发生时间早于统计结束时间)
        IncidentInstanceExample.Criteria orCriteria = example.or();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            orCriteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        orCriteria.andGmtRecoveryIsNull().andGmtOccurLessThan(new Date(endTimestamp));

        return incidentInstanceMapper.selectByExample(example);
    }

    private List<IncidentInstance> getIncidentIns(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        IncidentInstanceExample example = new IncidentInstanceExample();

        IncidentInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (startTimestamp != null) {
            criteria.andGmtLastOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtLastOccurLessThan(new Date(endTimestamp));
        }

        return incidentInstanceMapper.selectByExample(example);
    }

    private List<FailureInstance> getFailureIns(String appInstanceId, Long startTimestamp, Long endTimestamp) {
        FailureInstanceExample example = new FailureInstanceExample();
        FailureInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (startTimestamp != null) {
            criteria.andGmtOccurGreaterThanOrEqualTo(new Date(startTimestamp));
        }
        if (endTimestamp != null) {
            criteria.andGmtOccurLessThan(new Date(endTimestamp));
        }

        return failureInstanceMapper.selectByExample(example);
    }
}
