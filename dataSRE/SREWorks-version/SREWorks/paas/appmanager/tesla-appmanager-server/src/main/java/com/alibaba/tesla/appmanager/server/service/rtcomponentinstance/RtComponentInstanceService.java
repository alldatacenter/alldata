package com.alibaba.tesla.appmanager.server.service.rtcomponentinstance;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.componentinstance.ReportRtComponentInstanceStatusReq;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;

/**
 * 组件实例服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface RtComponentInstanceService {

    /**
     * 上报原始数据
     *
     * @param record 实时组件实例对象
     */
    void reportRaw(RtComponentInstanceDO record);

    /**
     * 上报 Component 实例状态
     *
     * @param request     上报数据请求
     * @param ignoreError 是否忽略错误 true or false，错误时抛出 AppException
     */
    void report(ReportRtComponentInstanceStatusReq request, boolean ignoreError);

    /**
     * 上报 Component 实例状态 (忽略错误)
     *
     * @param request 上报数据请求
     */
    void report(ReportRtComponentInstanceStatusReq request);

    /**
     * 获取实时 component instance 状态列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    Pagination<RtComponentInstanceDO> list(RtComponentInstanceQueryCondition condition);

    /**
     * 获取实时 component instance 状态历史列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    Pagination<RtComponentInstanceHistoryDO> listHistory(RtComponentInstanceHistoryQueryCondition condition);

    /**
     * 查询当前的组件实例，如果存在则返回，否则返回 null
     *
     * @param condition  查询条件 (appId/componentType/componentName/clusterId/namespaceId/stageId 必选,
     *                   clusterId/namespaceId/stage 可为空)
     * @return 实时组件实例 DO 对象 or null
     */
    RtComponentInstanceDO get(RtComponentInstanceQueryCondition condition);

    /**
     * 查询当前的组件实例，如果存在则返回；否则新增并返回
     *
     * @param condition 查询条件
     * @return 查询或新建后的实时组件实例 DO 对象
     */
    RtComponentInstanceDO getOrCreate(
            RtComponentInstanceQueryCondition condition, String appInstanceId, String version);
}
