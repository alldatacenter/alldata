package com.alibaba.sreworks.health.api.alert;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.alert.AlertInstanceCreateReq;

import java.util.List;

public interface AlertInstanceService extends BasicApi {
    /**
     * 按照时间聚合告警实例
     * @return
     */
    List<JSONObject> getAlertsTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId);

    /**
     * 按照App查询告警实例
     * @return
     */
    List<JSONObject> getAlertsByApp(String appId);

    /**
     * 按照实体查询告警实例
     * @return
     */
    List<JSONObject> getAlertsByInstance(String appId, String appInstanceId);

    /**
     * 按照实体值查询告警实例
     * @return
     */
    List<JSONObject> getAlerts(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp);

    /**
     * 按照ID查询告警实例
     * @return
     */
    JSONObject getAlertById(Long id);

    /**
     * 告警实例存在
     * @param id
     * @return
     */
    boolean existAlert(Long id);

    /**
     * 告警实例不存在
     * @param id
     * @return
     */
    boolean notExistAlert(Long id);

    /**
     * 按照定义批量新建告警实例
     *  @param defId 定义ID
     *  @param alerts 告警数据
     * @return
     * @throws Exception
     */
    int pushAlerts(Integer defId, List<JSONObject> alerts) throws Exception;

    /**
     * 新建告警实例
     * @param req
     * @return
     * @throws Exception
     */
    int addAlert(AlertInstanceCreateReq req) throws Exception;

    /**
     * 删除告警实例
     * @param id
     * @return
     */
    int deleteAlert(Long id) throws Exception;
}
