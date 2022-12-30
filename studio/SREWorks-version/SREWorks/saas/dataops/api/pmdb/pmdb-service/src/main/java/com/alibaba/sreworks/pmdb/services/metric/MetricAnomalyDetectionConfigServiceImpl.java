package com.alibaba.sreworks.pmdb.services.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.metric.MetricAnomalyDetectionConfigService;
import com.alibaba.sreworks.pmdb.api.metric.MetricService;
import com.alibaba.sreworks.pmdb.common.exception.MetricAnomalyDetectionConfigExistException;
import com.alibaba.sreworks.pmdb.common.exception.MetricNotExistException;
import com.alibaba.sreworks.pmdb.domain.metric.MetricAnomalyDetectionConfig;
import com.alibaba.sreworks.pmdb.domain.metric.MetricAnomalyDetectionConfigExample;
import com.alibaba.sreworks.pmdb.domain.metric.MetricAnomalyDetectionConfigMapper;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricAnomalyDetectionBaseReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricAnomalyDetectionCreateReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 指标Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 16:18
 */
@Slf4j
@Service
public class MetricAnomalyDetectionConfigServiceImpl implements MetricAnomalyDetectionConfigService {

    @Autowired
    MetricAnomalyDetectionConfigMapper adConfigMapper;

    @Autowired
    MetricService metricService;

    @Override
    public JSONObject getConfigById(Integer configId) {
        MetricAnomalyDetectionConfig config = adConfigMapper.selectByPrimaryKey(configId);
        return convertToJSONObject(config);
    }

    @Override
    public List<JSONObject> getConfigByMetric(Integer metricId) {
        MetricAnomalyDetectionConfigExample example = new MetricAnomalyDetectionConfigExample();
        example.createCriteria().andMetricIdEqualTo(metricId);

        List<MetricAnomalyDetectionConfig> configs = adConfigMapper.selectByExampleWithBLOBs(example);
        return convertToJSONObjects(configs);
    }

    @Override
    public JSONObject getConfigByMetricRule(Integer metricId, Integer ruleId) {
        MetricAnomalyDetectionConfigExample example = new MetricAnomalyDetectionConfigExample();
        example.createCriteria().andMetricIdEqualTo(metricId).andRuleIdEqualTo(ruleId);

        List<MetricAnomalyDetectionConfig> configs = adConfigMapper.selectByExampleWithBLOBs(example);
        if (configs == null) {
            return convertToJSONObject(null);
        } else {
            return convertToJSONObject(configs.get(0));
        }

    }

    @Override
    public int createConfig(MetricAnomalyDetectionCreateReq req) throws Exception {
        MetricAnomalyDetectionConfig config = buildAnomalyDetectionConfig(req);
        if(getConfigByMetricRule(config.getMetricId(), config.getRuleId()).isEmpty()) {
            return adConfigMapper.insert(config);
        } else {
            throw new MetricAnomalyDetectionConfigExistException("指标检测配置已经存在");
        }
    }

    @Override
    public int deleteConfigById(Integer configId) {
        return adConfigMapper.deleteByPrimaryKey(configId);
    }

    private MetricAnomalyDetectionConfig buildAnomalyDetectionConfig(MetricAnomalyDetectionBaseReq req) throws Exception {
        MetricAnomalyDetectionConfig config = new MetricAnomalyDetectionConfig();

        JSONObject metricObject = metricService.getMetricById(req.getMetricId());
        if (metricObject.isEmpty()) {
            throw new MetricNotExistException(String.format("指标 id:%s 不存在", req.getMetricId()));
        }

        Date now = new Date();
        config.setGmtCreate(now);
        config.setGmtModified(now);
//        config.setTitle(req.getTitle());
        config.setRuleId(req.getRuleId());
        config.setMetricId(req.getMetricId());
        config.setEnable(req.getEnable());
        config.setCreator(req.getCreator());
        config.setOwners(req.getOwners());
        config.setDescription(req.getDescription());

        return config;
    }
}
