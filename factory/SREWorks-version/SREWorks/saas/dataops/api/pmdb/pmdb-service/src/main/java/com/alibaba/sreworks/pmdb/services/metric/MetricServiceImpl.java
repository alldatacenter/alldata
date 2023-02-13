package com.alibaba.sreworks.pmdb.services.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.metric.MetricService;
import com.alibaba.sreworks.pmdb.common.exception.MetricExistException;
import com.alibaba.sreworks.pmdb.common.exception.MetricNotExistException;
import com.alibaba.sreworks.pmdb.domain.metric.*;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricBaseReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 指标Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 16:18
 */
@Slf4j
@Service
public class MetricServiceImpl implements MetricService {

    @Autowired
    MetricMapper metricMapper;

    @Autowired
    MetricInstanceMapper metricInstanceMapper;

    @Autowired
    MetricAnomalyDetectionConfigMapper adConfigMapper;

    @Override
    public JSONObject getMetricById(Integer id) {
        Metric metric = metricMapper.selectByPrimaryKey(id);
        return convertToJSONObject(metric);
    }

    @Override
    public List<JSONObject> getMetricsByName(String name) {
        return getMetricsByLabels(name, null);
    }

    @Override
    public List<JSONObject> getMetricsByLabels(String name, JSONObject labels) {
        return getMetrics(name, labels);
    }

    @Override
    public List<JSONObject> getMetrics(String name, JSONObject labels) {
        MetricExample example = new MetricExample();
        if (StringUtils.isNotEmpty(name)) {
            example.createCriteria().andNameEqualTo(name);
        }
        List<Metric> metrics = metricMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(labels)) {
            return convertToJSONObjects(metrics);
        } else {
            List<Metric> restMetrics = metrics.parallelStream().filter(metric -> {
                JSONObject metricLabels = JSONObject.parseObject(metric.getLabels());
                Set<String> labelKeys = labels.keySet();
                for (String labelKey : labelKeys) {
                    if (metricLabels.containsKey(labelKey)) {
                        if (!metricLabels.getString(labelKey).equals(labels.getString(labelKey))) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            return convertToJSONObjects(restMetrics);
        }
    }

    @Override
    public int createMetric(MetricCreateReq req) throws Exception {
        Metric metric = buildMetric(req);
        if (getMetricByUid(metric.getUid()) != null) {
            throw new MetricExistException(String.format("指标已经存在[name:%s, labels:%s]", req.getName(), req.getLabels()));
        }
        metricMapper.insert(metric);
        return metric.getId();
    }

    @Override
    @Transactional
    public int updateMetric(MetricUpdateReq req) throws Exception {
        Metric existMetric = metricMapper.selectByPrimaryKey(req.getId());
        if (existMetric == null) {
            throw new MetricNotExistException(String.format("指标不存在[id:%s]", req.getId()));
        }

        Metric metric = new Metric();
        metric.setGmtModified(new Date());
        metric.setId(req.getId());
        metric.setName(StringUtils.isEmpty(req.getName()) ? existMetric.getName() : req.getName());
        metric.setAlias(req.getAlias());
        metric.setType(req.getType());
        metric.setLabels(req.getLabels() == null ? existMetric.getLabels() : JSONObject.toJSONString(req.getLabels()));
        metric.setLastModifier(req.getLastModifier());
        metric.setDescription(req.getDescription());
        metric.setUid(generateUid(metric.getName(), JSONObject.parseObject(metric.getLabels())));

        // 指标身份ID变更,需要确认是否有冲突指标定义
        if (!existMetric.getUid().equals(metric.getUid())) {
            if (getMetricByUid(metric.getUid()) != null) {
                throw new MetricNotExistException(String.format("指标已经存在[name:%s, labels:%s]", metric.getName(), metric.getLabels()));
            }
        }

        metricMapper.updateByPrimaryKeySelective(metric);
        return metric.getId();
    }

    @Override
    @Transactional
    public int deleteMetricById(Integer metricId) {
        MetricInstanceExample instanceExample = new MetricInstanceExample();
        instanceExample.createCriteria().andMetricIdEqualTo(metricId);
        metricInstanceMapper.deleteByExample(instanceExample);

//        RowBounds instanceRowBounds = new RowBounds(0, 1);
//        List<MetricInstance> metricInstances = metricInstanceMapper.selectByExampleWithRowbounds(instanceExample, instanceRowBounds);
//        if (metricInstances != null && !metricInstances.isEmpty()) {
//            // exist metric instance
//            throw new MetricInstanceExistException("指标实例不为空, 禁止删除");
//        }

        return metricMapper.deleteByPrimaryKey(metricId);
    }

    private Metric buildMetric(MetricBaseReq req) {
        Metric metric = new Metric();
        Date now = new Date();
        metric.setGmtCreate(now);
        metric.setGmtModified(now);
        metric.setName(req.getName());
        metric.setAlias(req.getAlias());
        metric.setType(req.getType());
        metric.setLabels(JSONObject.toJSONString(req.getLabels()));
        metric.setUid(generateUid(req.getName(), req.getLabels()));
        metric.setCreator(req.getCreator());
        metric.setLastModifier(req.getLastModifier());
        metric.setDescription(req.getDescription());

        return metric;
    }

    private Metric getMetricByUid(String Uid) {
        MetricExample example = new MetricExample();
        example.createCriteria().andUidEqualTo(Uid);
        List<Metric> metrics = metricMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(metrics)) {
            return metrics.get(0);
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
}
