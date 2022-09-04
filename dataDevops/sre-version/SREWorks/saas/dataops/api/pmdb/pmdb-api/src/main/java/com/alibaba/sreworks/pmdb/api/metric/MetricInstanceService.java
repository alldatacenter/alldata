package com.alibaba.sreworks.pmdb.api.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.BasicApi;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricDataReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceUpdateReq;
import org.springframework.util.DigestUtils;

import java.util.List;

public interface MetricInstanceService extends BasicApi {

    /**
     * 根据ID查询实例
     * @param id
     * @return
     */
    JSONObject getInstanceById(Long id);


    /**
     * 根据实例唯一身份ID查询实例
     * @param insId
     * @return
     */
    JSONObject getInstanceByUid(String uid);

    /**
     * 根据指标ID查询实例
     * @param metricId
     * @return
     */
    List<JSONObject> getInstanceByMetric(Integer metricId);

    /**
     * 根据指标标签查询实例
     * @param metricId
     * @return
     */
    List<JSONObject> getInstanceByLabels(Integer metricId, JSONObject labels);

    /**
     * 根据指标标签查询实例
     * @return
     */
    List<JSONObject> getInstanceByLabels(JSONObject labels, Integer size);

    /**
     * 新建指标
     * @param req
     */
    Long createInstance(MetricInstanceCreateReq req) throws Exception ;

    /**
     * 更新指标
     * @param req
     */
    Long updateInstance(MetricInstanceUpdateReq req) throws Exception ;

    /**
     * 删除指标
     * @param id
     * @return
     */
    int deleteInstanceById(Long id);

    /**
     * 删除指标
     * @param metricId
     * @return
     */
    int deleteInstanceByMetric(Integer metricId);


    /**
     * 推送指标实例
     * @param metricId
     * @param isInsertNewIns 插入新实例
     * @param isPushQueue 推送采集数据到消息队列
     * @param pushData
     */
    void pushData(Integer metricId, boolean isInsertNewIns, boolean isPushQueue, MetricDataReq pushData) throws Exception ;

    /**
     * 推送指标实例
     * @param metricId
     * @param isInsertNewIns 插入新实例
     * @param isDeleteOldIns 删除旧实例
     * @param isPushQueue 推送采集数据到消息队列
     * @param pushDatas
     */
    void pushDatas(Integer metricId, boolean isInsertNewIns, boolean isDeleteOldIns, boolean isPushQueue, List<MetricDataReq> pushDatas) throws Exception ;

    /**
     * 生成指标实例身份ID
     * @param
     * @return
     */
    default String generateUid(Integer metricId, JSONObject labels) {
        return DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels)).getBytes());
    }
}
