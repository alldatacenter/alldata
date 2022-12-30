package com.alibaba.sreworks.health.services.instance.risk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.risk.RiskInstanceService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionNotExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.common.exception.RiskInstanceNotExistException;
import com.alibaba.sreworks.health.domain.CommonDefinitionMapper;
import com.alibaba.sreworks.health.domain.RiskInstance;
import com.alibaba.sreworks.health.domain.RiskInstanceExample;
import com.alibaba.sreworks.health.domain.RiskInstanceMapper;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceBaseReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceUpdateReq;
import com.alibaba.sreworks.health.services.instance.InstanceService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险实例service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 16:30
 */
@Service
@Slf4j
public class RiskInstanceServiceImpl extends InstanceService implements RiskInstanceService {

    @Autowired
    RiskInstanceMapper riskInstanceMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Override
    public List<JSONObject> getRisksByApp(String appId) {
        return getRisksByInstance(appId, null);
    }

    @Override
    public List<JSONObject> getRisksByInstance(String appId, String appInstanceId) {
        return getRisks(appId, appInstanceId, null, null, null, null);
    }

    @Override
    public List<JSONObject> getRisks(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp) {
        List<Integer> defIds = getDefIds(appId, appComponentName, Constant.RISK);
        if (CollectionUtils.isEmpty(defIds)) {
            return convertToJSONObjects(null);
        }

        RiskInstanceExample example = new RiskInstanceExample();
        RiskInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
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

        List<JSONObject> results = convertToJSONObjects(riskInstanceMapper.selectByExampleWithBLOBs(example));
        return richInstances(results);
    }

    @Override
    public List<JSONObject> getRisksTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId) {
        // TODO 按照日期缓存统计数据
        Map<String, Object> params = buildTimeAggParams(timeUnit, startTimestamp, endTimestamp, appInstanceId);
        List<JSONObject> results = riskInstanceMapper.countGroupByTime(params);
        return convertToJSONObjects(richTimeAgg(timeUnit, startTimestamp, endTimestamp, results));
    }

    @Override
    public JSONObject getRiskById(Long id) {
        RiskInstance riskInstance = riskInstanceMapper.selectByPrimaryKey(id);
        JSONObject result = convertToJSONObject(riskInstance);
        return richInstance(result);
    }

    @Override
    public boolean existRisk(Long id) {
        return riskInstanceMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistRisk(Long id) {
        return !existRisk(id);
    }

    @Override
    public int pushRisks(Integer defId, List<JSONObject> risks) throws Exception {
        if (CollectionUtils.isEmpty(risks)) {
            return 0;
        } else if (risks.size() > Constant.MAX_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_DATA_FLUSH_SIZE));
        }

        if (!existRefDefinition(defId, Constant.RISK)) {
            throw new CommonDefinitionNotExistException(String.format("风险定义[id:%s]不存在", defId));
        }

        List<RiskInstance> riskInstances = risks.parallelStream().map(risk -> {
            RiskInstanceCreateReq req = JSONObject.toJavaObject(risk, RiskInstanceCreateReq.class);
            req.setDefId(defId);
            return buildRiskInstanceSimple(req);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(riskInstances)) {
            return 0;
        }

        List<List<RiskInstance>> riskInstancesList = Lists.partition(riskInstances, Constant.DATA_FLUSH_STEP);
        riskInstancesList.forEach(items -> riskInstanceMapper.batchInsert(items));
        return riskInstances.size();
    }

    @Override
    public int addRisk(RiskInstanceCreateReq req) throws Exception {
        RiskInstance riskInstance = buildRiskInstance(req);
        return riskInstanceMapper.insert(riskInstance);
    }

    @Override
    public int updateRisk(RiskInstanceUpdateReq req) throws Exception {
        RiskInstance existRiskInstance = riskInstanceMapper.selectByPrimaryKey(req.getId());
        if (existRiskInstance == null) {
            throw new RiskInstanceNotExistException(String.format("更新风险实例[id:%s]不存在", req.getId()));
        }

        req.setDefId(req.getDefId() == null ? existRiskInstance.getDefId() : req.getDefId());

        RiskInstance riskInstance = buildRiskInstance(req);
        riskInstance.setId(req.getId());
        riskInstance.setGmtCreate(null);

        return riskInstanceMapper.updateByPrimaryKey(riskInstance);
    }

    @Override
    public int deleteRisk(Long id) throws Exception {
        return riskInstanceMapper.deleteByPrimaryKey(id);
    }

    private RiskInstance buildRiskInstance(RiskInstanceBaseReq req) throws Exception {
        if (!existRefDefinition(req.getDefId(), Constant.RISK)) {
            throw new CommonDefinitionNotExistException(String.format("风险定义[id:%s]不存在", req.getDefId()));
        }
        return buildRiskInstanceSimple(req);
    }

    private RiskInstance buildRiskInstanceSimple(RiskInstanceBaseReq req) {
        RiskInstance riskInstance = new RiskInstance();
        Date now = new Date();
        riskInstance.setGmtCreate(now);
        riskInstance.setGmtModified(now);
        riskInstance.setDefId(req.getDefId());
        riskInstance.setAppInstanceId(req.getAppInstanceId());
        riskInstance.setAppComponentInstanceId(req.getAppComponentInstanceId());
        riskInstance.setGmtOccur(req.getOccurTs() != null ? new Timestamp(req.getOccurTs()) : null);
        riskInstance.setSource(req.getSource());
        riskInstance.setContent(req.getContent());

        return riskInstance;
    }
}
