package com.alibaba.sreworks.health.api.failure;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.failure.FailureInstanceCreateReq;

import java.util.List;

public interface FailureInstanceService extends BasicApi {
    /**
     * 按照时间聚合故障实例
     * @return
     */
    List<JSONObject> getFailuresTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId);

    /**
     * 按照ID查询故障实例
     * @return
     */
    JSONObject getFailureById(Long id);

    /**
     * 按照应用查询故障实例列表
     * @return
     */
    List<JSONObject> getFailuresByApp(String appId);

    /**
     * 按照定义查询故障实例列表
     * @return
     */
    List<JSONObject> getFailuresByDefinition(Integer defId);

    /**
     * 查询故障实例
     * @param appId
     * @param appInstanceId
     * @param appComponentName
     * @return
     */
    List<JSONObject> getFailures(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp);

    /**
     * 故障实例存在
     * @param id
     * @return
     */
    boolean existFailure(Long id);

    /**
     * 故障实例不存在
     * @param id
     * @return
     */
    boolean notExistFailure(Long id);

    /**
     * 推送故障实例(不存在时新建，存在按规则升级故障等级)
     * @param failure
     * @return
     * @throws Exception
     */
    int pushFailure(Integer defId, JSONObject failure) throws Exception;

    /**
     * 新建故障实例
     * @param req
     * @return
     * @throws Exception
     */
    int addFailure(FailureInstanceCreateReq req) throws Exception;

    /**
     * 更新故障实例故障等级
     * @param id
     * @param level
     * @return
     * @throws Exception
     */
    int updateFailureLevel(Long id, String level) throws Exception;

    /**
     * 升级故障实例(逐级升级)
     * @param id
     * @return
     * @throws Exception
     */
    int upgradeFailureLevel(Long id) throws Exception;
}
