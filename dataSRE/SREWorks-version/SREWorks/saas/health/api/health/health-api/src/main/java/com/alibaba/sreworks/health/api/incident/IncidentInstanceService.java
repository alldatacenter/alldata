package com.alibaba.sreworks.health.api.incident;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceHealingReq;

import java.util.List;

public interface IncidentInstanceService extends BasicApi {
    /**
     * 按照时间聚合异常实例
     * @return
     */
    List<JSONObject> getIncidentsTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId);

    /**
     * 按照App查询异常实例
     * @return
     */
    List<JSONObject> getIncidentsByApp(String appId);

    /**
     * 按照实体值查询异常实例
     * @return
     */
    List<JSONObject> getIncidents(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Integer incidentTypeId, Long startTimestamp, Long endTimestamp);

    /**
     * 按照异常链路查询异常实例
     * @return
     */
    List<JSONObject> getIncidentsGroupByTrace(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Integer incidentTypeId, String traceId, Long startTimestamp, Long endTimestamp);

    /**
     * 按照异常链路查询异常实例
     * @return
     */
    List<JSONObject> getIncidentsByTrace(String traceId);

    /**
     * 按照ID查询异常实例
     * @return
     */
    JSONObject getIncidentById(Long id);

    /**
     * 异常实例存在
     * @param id
     * @return
     */
    boolean existIncident(Long id);

    /**
     * 异常实例不存在
     * @param id
     * @return
     */
    boolean notExistIncident(Long id);

    /**
     * 推送异常实例
     * @return
     * @throws Exception
     */
    JSONObject pushIncident(Integer defId, JSONObject incident) throws Exception;

    /**
     * 新建异常实例
     * @param req
     * @return
     * @throws Exception
     */
    JSONObject addIncident(IncidentInstanceCreateReq req) throws Exception;

    /**
     * 恢复异常
     * @param defId
     * @param appInstanceId
     * @param appComponentInstanceId
     * @return
     * @throws Exception
     */
    boolean recoveryIncident(Integer defId, String appInstanceId, String appComponentInstanceId) throws Exception;

    /**
     * 恢复异常
     * @param id
     * @return
     */
    boolean recoveryIncident(Long id);

    /**
     * 更新自愈信息
     * @param defId
     * @param appInstanceId
     * @param appComponentInstanceId
     * @param req
     * @return
     * @throws Exception
     */
    JSONObject updateIncidentSelfHealing(Integer defId, String appInstanceId, String appComponentInstanceId, IncidentInstanceHealingReq req) throws Exception;

    /**
     * 更新自愈信息
     * @param traceId
     * @param req
     * @return
     * @throws Exception
     */
    List<JSONObject> updateIncidentSelfHealing(String traceId, IncidentInstanceHealingReq req) throws Exception;


    /**
     * 更新自愈信息
     * @param id
     * @param req
     * @return
     * @throws Exception
     */
    JSONObject updateIncidentSelfHealing(Long id, IncidentInstanceHealingReq req) throws Exception;
    
    /**
     * 删除异常实例
     * @param id
     * @return
     * @throws Exception
     */
    int deleteIncident(Long id) throws Exception;

    /**
     * 删除异常实例
     * @param traceId
     * @return
     * @throws Exception
     */
    int deleteIncidentsByTrace(String traceId) throws Exception;
}
