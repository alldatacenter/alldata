package com.alibaba.sreworks.health.api.risk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceUpdateReq;

import java.util.List;

public interface RiskInstanceService extends BasicApi {
    /**
     * 按照App查询风险实例
     * @return
     */
    List<JSONObject> getRisksByApp(String appId);

    /**
     * 按照实体查询风险实例
     * @return
     */
    List<JSONObject> getRisksByInstance(String appId, String appInstanceId);

    /**
     * 按照实体值查询风险实例
     * @return
     */
    List<JSONObject> getRisks(String appId, String appInstanceId, String appComponentName, String appComponentInstanceId, Long startTimestamp, Long endTimestamp);

    /**
     * 按照时间聚合风险实例
     * @return
     */
    List<JSONObject> getRisksTimeAgg(String timeUnit, Long startTimestamp, Long endTimestamp, String appInstanceId);

    /**
     * 按照ID查询风险实例
     * @return
     */
    JSONObject getRiskById(Long id);

    /**
     * 风险实例存在
     * @param id
     * @return
     */
    boolean existRisk(Long id);

    /**
     * 风险实例不存在
     * @param id
     * @return
     */
    boolean notExistRisk(Long id);

    /**
     * 按照定义批量新建风险实例
     *  @param defId 定义ID
     *  @param risks 风险数据
     * @return
     * @throws Exception
     */
    int pushRisks(Integer defId, List<JSONObject> risks) throws Exception;

    /**
     * 新建风险实例
     * @param req
     * @return
     * @throws Exception
     */
    int addRisk(RiskInstanceCreateReq req) throws Exception;

    /**
     * 更新风险实例
     * @param req
     * @return
     * @throws Exception
     */
    int updateRisk(RiskInstanceUpdateReq req) throws Exception;

    /**
     * 删除风险实例
     * @param id
     * @return
     */
    int deleteRisk(Long id) throws Exception ;
}
