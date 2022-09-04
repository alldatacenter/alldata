package com.alibaba.sreworks.pmdb.api.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.BasicApi;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricUpdateReq;
import org.springframework.util.DigestUtils;

import java.util.List;

public interface MetricService extends BasicApi {

    /**
     * 根据指标ID查询
     * @param id
     * @return
     */
    JSONObject getMetricById(Integer id);

    /**
     * 按照指标名称查询指标
     * @return
     */
    List<JSONObject> getMetricsByName(String name);

    /**
     * 按照应用指标查询指标
     * @return
     */
    List<JSONObject> getMetricsByLabels(String name, JSONObject labels);

    /**
     * 查询所有指标
     * @return
     */
    List<JSONObject> getMetrics(String name, JSONObject labels);

    /**
     * 新建指标
     * @param req
     */
    int createMetric(MetricCreateReq req) throws Exception;

    /**
     * 新建指标
     * @param req
     */
    int updateMetric(MetricUpdateReq req) throws Exception;

    /**
     * 删除指标
     * @param metricId
     * @return
     */
    int deleteMetricById(Integer metricId);

    /**
     * 生成指标定义身份ID
     * @param
     * @return
     */
    default String generateUid(String metricName, JSONObject labels) {
        StringBuilder content = new StringBuilder();
        content.append(metricName).append("|").append(JSONObject.toJSONString(labels));
        return DigestUtils.md5DigestAsHex(content.toString().getBytes());
    }
}
