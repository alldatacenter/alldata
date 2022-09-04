package com.alibaba.sreworks.health.services.instance.event;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.event.EventInstanceService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.CommonDefinitionNotExistException;
import com.alibaba.sreworks.health.common.exception.EventInstanceNotExistException;
import com.alibaba.sreworks.health.common.exception.ParamException;
import com.alibaba.sreworks.health.domain.CommonDefinitionMapper;
import com.alibaba.sreworks.health.domain.EventInstance;
import com.alibaba.sreworks.health.domain.EventInstanceExample;
import com.alibaba.sreworks.health.domain.EventInstanceMapper;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceBaseReq;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceUpdateReq;
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
import java.util.stream.Collectors;

/**
 * 事件实例service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 16:30
 */
@Service
@Slf4j
public class EventInstanceServiceImpl extends InstanceService implements EventInstanceService {

    @Autowired
    EventInstanceMapper eventInstanceMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Override
    public List<JSONObject> getEventsByApp(String appId, Long sTimestamp, Long eTimestamp) {
        return getEventsByInstance(appId, null,sTimestamp, eTimestamp);
    }

    @Override
    public List<JSONObject> getEventsByInstance(String appId, String appInstanceId, Long sTimestamp, Long eTimestamp) {
        return getEvents(appId, appInstanceId, null, null, sTimestamp, eTimestamp);
    }

    @Override
    public List<JSONObject> getEvents(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long sTimestamp, Long eTimestamp) {
        List<Integer> defIds = getDefIds(appId, appComponentName, Constant.EVENT);
        if (CollectionUtils.isEmpty(defIds)) {
            return convertToJSONObjects(null);
        }

        EventInstanceExample example = new EventInstanceExample();
        EventInstanceExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotEmpty(appInstanceId)) {
            criteria.andAppInstanceIdEqualTo(appInstanceId);
        }
        if (StringUtils.isNotEmpty(appComponentInstanceId)) {
            criteria.andAppComponentInstanceIdEqualTo(appComponentInstanceId);
        }
        if (sTimestamp != null) {
            criteria.andGmtOccurGreaterThanOrEqualTo(new Date(sTimestamp));
        }
        if (eTimestamp != null) {
            criteria.andGmtCreateLessThanOrEqualTo(new Date(eTimestamp));
        }
        criteria.andDefIdIn(defIds);

        List<JSONObject> results = convertToJSONObjects(eventInstanceMapper.selectByExampleWithBLOBs(example));
        return richInstances(results);
    }

    @Override
    public JSONObject getEventById(Long id) {
        EventInstance riskInstance = eventInstanceMapper.selectByPrimaryKey(id);
        JSONObject result = convertToJSONObject(riskInstance);
        return richInstance(result);
    }

    @Override
    public boolean existEvent(Long id) {
        return eventInstanceMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistEvent(Long id) {
        return !existEvent(id);
    }

    @Override
    public int pushEvents(Integer defId, List<JSONObject> events) throws Exception {
        if (CollectionUtils.isEmpty(events)) {
            return 0;
        } else if (events.size() > Constant.MAX_DATA_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_DATA_FLUSH_SIZE));
        }

        if (!existRefDefinition(defId, Constant.EVENT)) {
            throw new CommonDefinitionNotExistException(String.format("事件定义[id:%s]不存在", defId));
        }

        List<EventInstance> eventInstances = events.parallelStream().map(event -> {
            EventInstanceCreateReq req = JSONObject.toJavaObject(event, EventInstanceCreateReq.class);
            req.setDefId(defId);
            return buildEventInstanceSimple(req);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(eventInstances)) {
            return 0;
        }

        List<List<EventInstance>> eventInstancesList =  Lists.partition(eventInstances, Constant.DATA_FLUSH_STEP);
        eventInstancesList.forEach(items -> eventInstanceMapper.batchInsert(items));
        return eventInstances.size();
    }

    @Override
    public int addEvent(EventInstanceCreateReq req) throws Exception {
        EventInstance eventInstance = buildEventInstance(req);
        return eventInstanceMapper.insert(eventInstance);
    }

    @Override
    public int updateEvent(EventInstanceUpdateReq req) throws Exception {
        EventInstance existEventInstance = eventInstanceMapper.selectByPrimaryKey(req.getId());
        if (existEventInstance == null) {
            throw new EventInstanceNotExistException(String.format("更新事件实例[id:%s]不存在", req.getId()));
        }

        req.setDefId(req.getDefId() == null ? existEventInstance.getDefId() : req.getDefId());

        EventInstance eventInstance = buildEventInstance(req);
        eventInstance.setId(req.getId());
        eventInstance.setGmtCreate(null);

        return eventInstanceMapper.updateByPrimaryKey(eventInstance);
    }

    @Override
    public int deleteEvent(Long id) throws Exception {
        return eventInstanceMapper.deleteByPrimaryKey(id);
    }

    private EventInstance buildEventInstance(EventInstanceBaseReq req) throws Exception {
        if (!existRefDefinition(req.getDefId(), Constant.EVENT)) {
            throw new CommonDefinitionNotExistException(String.format("事件定义[id:%s]不存在", req.getDefId()));
        }
        return buildEventInstanceSimple(req);
    }

    private EventInstance buildEventInstanceSimple(EventInstanceBaseReq req) {
        EventInstance eventInstance = new EventInstance();
        Date now = new Date();
        eventInstance.setGmtCreate(now);
        eventInstance.setGmtModified(now);
        eventInstance.setDefId(req.getDefId());
        eventInstance.setAppInstanceId(req.getAppInstanceId());
        eventInstance.setAppComponentInstanceId(req.getAppComponentInstanceId());
        eventInstance.setGmtOccur(req.getOccurTs() != null ? new Timestamp(req.getOccurTs()) : null);
        eventInstance.setSource(req.getSource());
        eventInstance.setType(req.getType());
        eventInstance.setContent(req.getContent());

        return eventInstance;
    }
}
