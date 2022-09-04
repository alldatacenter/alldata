package com.alibaba.tesla.appmanager.server.service.rtappinstance;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;

import java.util.Set;

/**
 * 实时应用实例服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface RtAppInstanceService {

    /**
     * 获取当前需要更新的实例状态的 appInstanceId 集合
     *
     * @return 集合
     */
    Set<String> getStatusUpdateSet();

    /**
     * 在状态更新集合中删除指定的 appInstanceId
     *
     * @param appInstanceId 应用实例 ID
     */
    void removeStatusUpdateSet(String appInstanceId);

    /**
     * 触发指定 app instance 的异步状态更新 (时间窗口内自动去重)
     *
     * @param appInstanceId 应用实例 ID
     */
    void asyncTriggerStatusUpdate(String appInstanceId);

    /**
     * 触发指定 app instance 的同步状态更新
     *
     * @param appInstanceId 应用实例 ID
     */
    void syncTriggerStatusUpdate(String appInstanceId);

    /**
     * 获取实时 app instance 状态历史列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    Pagination<RtAppInstanceHistoryDO> listHistory(RtAppInstanceHistoryQueryCondition condition);

    /**
     * 获取实时 app instance 状态列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    Pagination<RtAppInstanceDO> list(RtAppInstanceQueryCondition condition);

    /**
     * 查询当前的应用实例，如果存在则返回；否则新增并返回
     *
     * @param condition       查询条件 (appId/clusterId/namespaceId/stageId 必选, clusterId/namespaceId/stage 可为空)
     * @param appInstanceId   如果不存在的话，新增的应用实例 ID
     * @param appInstanceName 如果不存在的话，新增的应用实例名称
     * @param version         如果不存在的话，新增的应用实例版本
     * @return 查询或新建后的实时应用实例 DO 对象
     */
    RtAppInstanceDO getOrCreate(RtAppInstanceQueryCondition condition,
                                String appInstanceId, String appInstanceName, String version);

    /**
     * 查询当前的应用实例
     *
     * @param condition       查询条件 (appId/clusterId/namespaceId/stageId 必选, clusterId/namespaceId/stage 可为空)
     * @return 查询实时应用实例 DO 对象
     */
    RtAppInstanceDO get(RtAppInstanceQueryCondition condition);

    /**
     * 删除指定的应用实例
     *
     * @param appInstanceId 应用实例 ID
     * @return RtAppInstanceDO
     */
    int delete(String appInstanceId);
}
