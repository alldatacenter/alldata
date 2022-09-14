package com.alibaba.sreworks.health.services.instance.failure;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.failure.FailureInstanceService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionNotExistException;
import com.alibaba.sreworks.health.common.exception.FailureInstanceNotExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.common.exception.RequestException;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.req.failure.FailureInstanceCreateReq;
import com.alibaba.sreworks.health.services.instance.InstanceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 故障实例服务类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 20:55
 */
@Service
public class FailureInstanceServiceImpl extends InstanceService implements FailureInstanceService {

    @Autowired
    FailureInstanceMapper failureInstanceMapper;

    @Autowired
    FailureRecordMapper failureRecordMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    IncidentInstanceMapper incidentInstanceMapper;

    @Override
    public List<JSONObject> getFailuresTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId) {
        // TODO 按照日期缓存统计数据
        Map<String, Object> params = buildTimeAggParams(timeUnit, startTimestamp, endTimestamp, appInstanceId);
        List<JSONObject> results = failureInstanceMapper.countGroupByTime(params);
        return convertToJSONObjects(richTimeAgg(timeUnit, startTimestamp, endTimestamp, results));
    }

    @Override
    public JSONObject getFailureById(Long id) {
        FailureInstance failureInstance = failureInstanceMapper.selectByPrimaryKey(id);
        if (failureInstance == null) {
            return convertToJSONObject(null);
        }
        JSONObject result = convertToJSONObject(failureInstance);
        return richInstance(result);
    }

    @Override
    public List<JSONObject> getFailuresByApp(String appId) {
        return getFailures(appId, null, null, null, null, null);
    }

    @Override
    public List<JSONObject> getFailuresByDefinition(Integer defId) {
        return null;
    }

    @Override
    public List<JSONObject> getFailures(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp) {
        List<Integer> defIds = getDefIds(appId, appComponentName, Constant.FAILURE);
        if (CollectionUtils.isEmpty(defIds)) {
            return convertToJSONObjects(null);
        }

        FailureInstanceExample example = new FailureInstanceExample();
        FailureInstanceExample.Criteria criteria = example.createCriteria();
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

        List<JSONObject> results = convertToJSONObjects(failureInstanceMapper.selectByExample(example));
        return richInstances(results);
    }

    @Override
    public boolean existFailure(Long id) {
        return failureInstanceMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistFailure(Long id) {
        return !existFailure(id);
    }

    @Override
    public int pushFailure(Integer defId, JSONObject failure) throws Exception {
        FailureInstanceCreateReq req = JSONObject.toJavaObject(failure, FailureInstanceCreateReq.class);
        req.setDefId(defId);
        // 确认是否已经存在故障实例(恢复时间为空的实例)
        FailureInstance aliveFailure = getAliveFailure(req.getDefId(), req.getAppInstanceId(), req.getAppComponentInstanceId());
        if (aliveFailure == null) {
            addFailure(req);
        } else {
            // 故障已经恢复
            if (req.getRecoveryTs() != null) {
                recoveryFailure(aliveFailure.getId(), req.getRecoveryTs());
            } else {
                // 故障升级
                if (!aliveFailure.getLevel().equals(req.getLevel())) {
                    updateFailureLevel(aliveFailure.getId(), req.getLevel());
                }
            }
        }
        return 1;
    }

    @Override
    @Transactional
    public int addFailure(FailureInstanceCreateReq req) throws Exception {
        if (!existRefDefinition(req.getDefId(), Constant.FAILURE)) {
            throw new CommonDefinitionNotExistException(String.format("故障定义[id:%s]不存在", req.getDefId()));
        }

        FailureInstance instance = new FailureInstance();
        Date now = new Date();
        instance.setGmtCreate(now);
        instance.setGmtModified(now);
        instance.setName(req.getName());
        instance.setDefId(req.getDefId());
        instance.setAppInstanceId(req.getAppInstanceId());
        instance.setAppComponentInstanceId(req.getAppComponentInstanceId());
        instance.setIncidentId(req.getIncidentId());
        instance.setGmtOccur(new Timestamp(req.getOccurTs()));
        instance.setGmtRecovery(req.getRecoveryTs() != null ? new Timestamp(req.getRecoveryTs()) : null);
        instance.setLevel(req.getLevel());
        instance.setContent(req.getContent());
        int result= failureInstanceMapper.insert(instance);

        addFailureRecordByInstance(instance, false);

        return result;
    }

    @Override
    @Transactional
    public int updateFailureLevel(Long id, String level) throws Exception {
        FailureInstance existFailureInstance = failureInstanceMapper.selectByPrimaryKey(id);
        if (existFailureInstance == null) {
            throw new FailureInstanceNotExistException(String.format("故障实例[id:%s]不存在", id));
        }
        if (StringUtils.isEmpty(level)) {
            throw new ParamException("故障等级为空");
        }
        if(!Constant.FAILURE_LEVEL_PATTERN.matcher(level).find()) {
            throw new ParamException("故障等级参数非法,目前仅支持[P4, P3, P2, P1, P0]");
        }

        level = level.toUpperCase();
        FailureInstance failureInstance = new FailureInstance();
        failureInstance.setId(id);
        failureInstance.setLevel(level);
        failureInstance.setGmtModified(new Date());
        int result = failureInstanceMapper.updateByPrimaryKeySelective(failureInstance);

        existFailureInstance.setLevel(level);
        addFailureRecordByInstance(existFailureInstance, true);

        return result;
    }

    @Override
    @Transactional
    public int upgradeFailureLevel(Long id) throws Exception {
        FailureInstance existFailureInstance = failureInstanceMapper.selectByPrimaryKey(id);
        if (existFailureInstance == null) {
            throw new FailureInstanceNotExistException(String.format("故障实例[id:%s]不存在", id));
        }

        int level = Integer.parseInt(existFailureInstance.getLevel().split("")[1]) - 1;
        if (level < Constant.MAX_FAILURE_LEVEL) {
            throw new RequestException("已达最高故障等级");
        }

        FailureInstance failureInstance = new FailureInstance();
        failureInstance.setId(id);
        failureInstance.setLevel(Constant.FAILURE_LEVEL_PREFIX + level);
        failureInstance.setGmtModified(new Date());
        int result = failureInstanceMapper.updateByPrimaryKeySelective(failureInstance);

        existFailureInstance.setLevel(failureInstance.getLevel());
        addFailureRecordByInstance(existFailureInstance, true);

        return result;
    }

    private int addFailureRecordByInstance(FailureInstance instance, boolean upgrade) {
        Date now = new Date();
        FailureRecord record = new FailureRecord();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        record.setFailureId(instance.getId());
        record.setDefId(instance.getDefId());
        record.setAppInstanceId(instance.getAppInstanceId());
        record.setAppComponentInstanceId(instance.getAppComponentInstanceId());
        record.setIncidentId(instance.getIncidentId());
        record.setName(instance.getName());
        record.setLevel(instance.getLevel());
        record.setContent(instance.getContent());
        record.setGmtOccur(instance.getGmtOccur());
        record.setGmtRecovery(instance.getGmtRecovery());

//        if (upgrade) {
//            record.setGmtUpgrade(now);
//        }

        return failureRecordMapper.insert(record);
    }

    private FailureInstance getAliveFailure(Integer defId, String appInstanceId, String appComponentInstanceId) throws Exception {
        FailureInstanceExample example = new FailureInstanceExample();
        if (StringUtils.isEmpty(appComponentInstanceId)) {
            example.createCriteria().andDefIdEqualTo(defId).andAppInstanceIdEqualTo(appInstanceId).
                    andGmtRecoveryIsNull();
        } else {
            example.createCriteria().andDefIdEqualTo(defId).andAppInstanceIdEqualTo(appInstanceId).
                    andAppComponentInstanceIdEqualTo(appComponentInstanceId).andGmtRecoveryIsNull();
        }

        List<FailureInstance> instances = failureInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        } else {
            if (instances.size() > 1) {
                throw new ParamException(String.format("待恢复故障实例不唯一,定义ID:%s,应用实例:%s,应用组件实例:%s", defId, appInstanceId, appComponentInstanceId));
            }
            return instances.get(0);
        }
    }

    private void recoveryFailure(Long failureInstanceId, Long recoveryTs) {
        FailureInstance failureInstance = new FailureInstance();
        failureInstance.setId(failureInstanceId);
        failureInstance.setGmtRecovery(new Timestamp(recoveryTs));
        failureInstanceMapper.updateByPrimaryKeySelective(failureInstance);

        FailureRecord failureRecord = new FailureRecord();
        failureRecord.setGmtRecovery(new Timestamp(recoveryTs));
        FailureRecordExample example = new FailureRecordExample();
        example.createCriteria().andFailureIdEqualTo(failureInstanceId);
        failureRecordMapper.updateByExampleSelective(failureRecord, example);
    }
}
