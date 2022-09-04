package com.alibaba.sreworks.pmdb.api.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.BasicApi;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricAnomalyDetectionCreateReq;

import java.util.List;

public interface MetricAnomalyDetectionConfigService extends BasicApi {

    /**
     * 根据ID查询检测配置
     * @param configId
     * @return
     */
    JSONObject getConfigById(Integer configId);

    /**
     * 根据指标查询检测配置
     * @param metricId
     * @return
     */
    List<JSONObject> getConfigByMetric(Integer metricId);

    /**
     * 根据指标和规则查询检测配置
     * @param metricId
     * @param ruleId
     * @return
     */
    JSONObject getConfigByMetricRule(Integer metricId, Integer ruleId);

    /**
     * 新建检测配置
     * @param req
     */
    int createConfig(MetricAnomalyDetectionCreateReq req) throws Exception ;

    /**
     * 删除检测配置
     * @param configId
     * @return
     */
    int deleteConfigById(Integer configId);
}
