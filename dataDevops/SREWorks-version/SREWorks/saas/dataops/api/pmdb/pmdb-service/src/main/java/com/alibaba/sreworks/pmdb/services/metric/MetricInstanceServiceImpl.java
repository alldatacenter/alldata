package com.alibaba.sreworks.pmdb.services.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.metric.MetricInstanceService;
import com.alibaba.sreworks.pmdb.common.constant.Constant;
import com.alibaba.sreworks.pmdb.common.exception.MetricInstanceExistException;
import com.alibaba.sreworks.pmdb.common.exception.MetricInstanceNotExistException;
import com.alibaba.sreworks.pmdb.common.exception.MetricNotExistException;
import com.alibaba.sreworks.pmdb.common.exception.ParamException;
import com.alibaba.sreworks.pmdb.common.properties.ApplicationProperties;
import com.alibaba.sreworks.pmdb.domain.metric.*;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricDataReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceBaseReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceUpdateReq;
import com.alibaba.sreworks.pmdb.producer.MetricDataKafkaProducer;
import com.alibaba.sreworks.pmdb.producer.MetricDataProducerRecorder;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 16:18
 */
@Slf4j
@Service
public class MetricInstanceServiceImpl implements MetricInstanceService {

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    MetricInstanceMapper metricInstanceMapper;

    @Autowired
    MetricMapper metricMapper;

    @Autowired
    MetricDataKafkaProducer kafkaProducer;

    @Override
    public JSONObject getInstanceById(Long id) {
        MetricInstance instance = metricInstanceMapper.selectByPrimaryKey(id);
        return convertToJSONObject(instance);
    }

    @Override
    public JSONObject getInstanceByUid(String uid) {
        MetricInstanceExample example = new MetricInstanceExample();
        example.createCriteria().andUidEqualTo(uid);

        List<MetricInstance> instances = metricInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return convertToJSONObject(null);
        } else {
            return convertToJSONObject(instances.get(0));
        }
    }

    @Override
    public List<JSONObject> getInstanceByMetric(Integer metricId) {
        MetricInstanceExample example = new MetricInstanceExample();
        example.createCriteria().andMetricIdEqualTo(metricId);

        List<MetricInstance> instances = metricInstanceMapper.selectByExample(example);
        return convertToJSONObjects(instances);
    }

    @Override
    public List<JSONObject> getInstanceByLabels(Integer metricId, JSONObject labels) {
        Metric metric = metricMapper.selectByPrimaryKey(metricId);
        if (Objects.isNull(metric)) {
            return convertToJSONObjects(null);
        }

        MetricInstanceExample example = new MetricInstanceExample();
        example.createCriteria().andMetricIdEqualTo(metricId);
        List<MetricInstance> instances = metricInstanceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(instances)) {
            return convertToJSONObjects(null);
        }

        List<MetricInstance> restMetricInstances = filterInstanceByLabels(instances, labels);
        return restMetricInstances.stream().map(metricInstance -> {
            JSONObject result = convertToJSONObject(metricInstance);
            result.put("metricName", metric.getName());
            result.put("metricAlias", metric.getAlias());
            result.put("metricType", metric.getType());
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public List<JSONObject> getInstanceByLabels(JSONObject labels, Integer size) {
        List<MetricInstance> instances = metricInstanceMapper.selectByExample(new MetricInstanceExample());
        if (CollectionUtils.isEmpty(instances)) {
            return convertToJSONObjects(null);
        }

        size = instances.size() > size ? size : instances.size();
        instances = instances.subList(0, size);
        List<MetricInstance> restMetricInstances = filterInstanceByLabels(instances, labels);
        if(CollectionUtils.isEmpty(restMetricInstances)) {
            return convertToJSONObjects(null);
        }

        List<Integer> metricIds = restMetricInstances.stream().map(MetricInstance::getMetricId).collect(Collectors.toList());
        MetricExample metricExample = new MetricExample();
        metricExample.createCriteria().andIdIn(metricIds);
        List<Metric> metrics = metricMapper.selectByExample(metricExample);
        Map<Integer, Metric> metricsMap = metrics.stream().collect(Collectors.toMap(Metric::getId, metric -> metric));

        return restMetricInstances.stream().map(metricInstance -> {
            JSONObject result = convertToJSONObject(metricInstance);
            Metric metric = metricsMap.get(metricInstance.getMetricId());
            result.put("metricName", metric.getName());
            result.put("metricAlias", metric.getAlias());
            result.put("metricType", metric.getType());
            return result;
        }).collect(Collectors.toList());
    }

    private List<MetricInstance> filterInstanceByLabels(List<MetricInstance> instances,  JSONObject labels) {
        return instances.parallelStream().filter(instance -> {
            JSONObject instanceLabels = JSONObject.parseObject(instance.getLabels());
            Set<String> labelKeys = labels.keySet();
            for (String labelKey : labelKeys) {
                if (instanceLabels.containsKey(labelKey)) {
                    if (!instanceLabels.getString(labelKey).equals(labels.getString(labelKey))) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public Long createInstance(MetricInstanceCreateReq req) throws Exception {
        MetricInstance instance = buildMetricInstance(req);
        if (getMetricInstanceByUid(instance.getUid()) != null) {
            throw new MetricInstanceExistException(String.format("指标实例[metricId: %s, labels:%s]已存在", instance.getMetricId(), instance.getLabels()));
        }
        metricInstanceMapper.insert(instance);
        return instance.getId();
    }

    @Override
    public Long updateInstance(MetricInstanceUpdateReq req) throws Exception {
        MetricInstance existMetricInstance = metricInstanceMapper.selectByPrimaryKey(req.getId());
        if (Objects.isNull(existMetricInstance)) {
            throw new MetricInstanceNotExistException(String.format("指标实例[id:%s]不存在", req.getId()));
        }
        Metric metric = metricMapper.selectByPrimaryKey(existMetricInstance.getMetricId());
        if (Objects.isNull(metric)) {
            throw new MetricNotExistException(String.format("指标[id:%s]不存在", req.getMetricId()));
        }

        MetricInstance metricInstance = new MetricInstance();
        metricInstance.setId(req.getId());
        metricInstance.setGmtModified(new Date());
        metricInstance.setDescription(req.getDescription());
        metricInstance.setLabels(req.getLabels() == null ? existMetricInstance.getLabels() : JSONObject.toJSONString(getInstanceLabels(metric, req.getLabels())));
        metricInstance.setUid(generateUid(existMetricInstance.getMetricId(), JSONObject.parseObject(metricInstance.getLabels())));

        // label变更
        if (!existMetricInstance.getUid().equals(metricInstance.getUid())) {
            if (getMetricInstanceByUid(metric.getUid()) != null) {
                throw new MetricInstanceExistException(String.format("指标实例[metricId:%s, labels:%s]已存在, 修改不生效", metricInstance.getMetricId(), metricInstance.getLabels()));
            }
        }

        metricInstanceMapper.updateByPrimaryKeySelective(metricInstance);
        return metricInstance.getId();
    }

    @Override
    public int deleteInstanceById(Long id) {
        return metricInstanceMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int deleteInstanceByMetric(Integer metricId) {
        MetricInstanceExample example = new MetricInstanceExample();
        example.createCriteria().andMetricIdEqualTo(metricId);
        return metricInstanceMapper.deleteByExample(example);
    }

    private JSONObject getInstanceLabels(Metric metric, JSONObject labels) {
        JSONObject metricLabels = JSONObject.parseObject(metric.getLabels());
        metricLabels.putAll(labels);
        return metricLabels;
    }

    private MetricInstance buildMetricInstance(MetricInstanceBaseReq req) throws Exception {
        MetricInstance instance = new MetricInstance();

        Metric metric = metricMapper.selectByPrimaryKey(req.getMetricId());
        if (Objects.isNull(metric)) {
            throw new MetricNotExistException(String.format("指标 id:%s 不存在", req.getMetricId()));
        }

        JSONObject labels = getInstanceLabels(metric, req.getLabels());
        Date now = new Date();
        instance.setGmtCreate(now);
        instance.setGmtModified(now);
        instance.setMetricId(req.getMetricId());
        instance.setLabels(JSONObject.toJSONString(labels));
        instance.setUid(generateUid(metric.getId(), labels));
        instance.setDescription(req.getDescription());

        return instance;
    }

    private MetricInstance getMetricInstanceByUid(String Uid) {
        MetricInstanceExample example = new MetricInstanceExample();
        example.createCriteria().andUidEqualTo(Uid);
        List<MetricInstance> metricInstances = metricInstanceMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(metricInstances)) {
            return metricInstances.get(0);
        } else {
            return null;
        }
    }

    @Override
    public JSONObject convertToJSONObject(Object obj) {
        if (obj == null) {
            return new JSONObject();
        }
        JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(obj));
        result.put("labels", JSONObject.parseObject(result.getString("labels")));
        return result;
    }

    @Override
    public void pushData(Integer metricId, boolean isInsertNewIns, boolean isPushQueue, MetricDataReq pushData) throws Exception {
        pushDatas(metricId, isInsertNewIns, false, isPushQueue, Collections.singletonList(pushData));
    }

    @Override
    public void pushDatas(Integer metricId, boolean isInsertNewIns, boolean isDeleteOldIns, boolean isPushQueue, List<MetricDataReq> pushDatas) throws Exception {
        if (CollectionUtils.isEmpty(pushDatas)) {
            return;
        } else if (pushDatas.size() > Constant.MAX_METRIC_INS_FLUSH_SIZE) {
            throw new ParamException(String.format("请进行分批写入, 单次最大允许%s条数据", Constant.MAX_METRIC_INS_FLUSH_SIZE));
        }

        Metric metric = metricMapper.selectByPrimaryKey(metricId);
        if (Objects.isNull(metric)) {
            throw new MetricNotExistException(String.format("指标[id:%s]不存在", metricId));
        }

        Map<String, JSONObject> instanceDataMap  = pushDatas.parallelStream().map(data -> {
            try {
                JSONObject instanceData = new JSONObject();
                JSONObject labels = getInstanceLabels(metric, data.getLabels());
                instanceData.put("metricId", metricId);
                instanceData.put("metricName", metric.getName());
                instanceData.put("type", metric.getType());
                instanceData.put("labels", labels);
                instanceData.put("uid", generateUid(metricId, labels));
                Long timestamp = data.getTimestamp();
                instanceData.put("timestamp", timestamp);
                instanceData.put("ts", timestamp);
                instanceData.put("value", data.getValue());
                return instanceData;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toMap(instanceData -> instanceData.getString("uid"), instanceData -> instanceData));

        if(isPushQueue) {
            MetricDataProducerRecorder recorder = new MetricDataProducerRecorder();
            List<String> instanceIds = instanceDataMap.values().parallelStream().map(instanceData -> {
                kafkaProducer.sendMsg(recorder.buildJsonValueRecord(applicationProperties.getKafkaMetricDataTopic(), instanceData));
                return instanceData.getString("instanceId");
            }).collect(Collectors.toList());
        }

        if (isInsertNewIns || isDeleteOldIns ) {
            MetricInstanceExample example = new MetricInstanceExample();
            example.createCriteria().andMetricIdEqualTo(metricId);
            Map<String, MetricInstance> oldInstanceMap = metricInstanceMapper.selectByExample(example).parallelStream().collect(Collectors.toMap(
                    MetricInstance::getUid, instance -> instance
            ));

            // 交集
            HashSet<String> resSet = new HashSet<>(oldInstanceMap.keySet());
            resSet.retainAll(instanceDataMap.keySet());

            int batchSize = 100;
            // 待删除实例
            if (isDeleteOldIns) {
                oldInstanceMap.keySet().removeAll(resSet);
                List<List<String>> deleteUidsList = Lists.partition(new ArrayList<>(oldInstanceMap.keySet()), batchSize);
                for (List<String> batchDeleteUids : deleteUidsList) {
                    MetricInstanceExample deleteExample = new MetricInstanceExample();
                    deleteExample.createCriteria().andUidIn(batchDeleteUids);
                    metricInstanceMapper.deleteByExample(deleteExample);
                }
            }

            // 待新增
            if (isInsertNewIns) {
                instanceDataMap.keySet().removeAll(resSet);
                Date now = new Date();
                List<MetricInstance> newInstances = instanceDataMap.values().parallelStream().
                        map( data -> {
                            MetricInstance instance = JSONObject.toJavaObject(data, MetricInstance.class);
                            instance.setGmtCreate(now);
                            instance.setGmtModified(now);
                            return instance;
                        }).
                        collect(Collectors.toList());
                List<List<MetricInstance>> insertInstancesList = Lists.partition(newInstances, batchSize);
                for(List<MetricInstance> insertInstances : insertInstancesList) {
                    metricInstanceMapper.batchInsert(insertInstances);
                }
            }
        }
    }
}
