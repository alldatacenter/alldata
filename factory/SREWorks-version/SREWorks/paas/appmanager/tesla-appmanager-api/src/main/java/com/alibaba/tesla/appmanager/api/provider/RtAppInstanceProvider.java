package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.RtAppInstanceDTO;
import com.alibaba.tesla.appmanager.domain.dto.RtAppInstanceHistoryDTO;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceDTO;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceHistoryDTO;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtAppInstanceHistoryQueryReq;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtAppInstanceQueryReq;
import com.alibaba.tesla.appmanager.domain.req.rtappinstance.RtComponentInstanceHistoryQueryReq;

/**
 * 实时应用实例 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface RtAppInstanceProvider {

    /**
     * 根据条件查询实时应用实例
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<RtAppInstanceDTO> queryByCondition(RtAppInstanceQueryReq request);

    /**
     * 根据条件查询实时应用实例历史
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<RtAppInstanceHistoryDTO> queryAppInstanceHistoryByCondition(RtAppInstanceHistoryQueryReq request);

    /**
     * 根据条件查询实时组件实例历史
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<RtComponentInstanceHistoryDTO>
    queryComponentInstanceHistoryByCondition(RtComponentInstanceHistoryQueryReq request);

    /**
     * 获取指定 appInstanceId 对应的应用实例状态详情
     *
     * @param appInstanceId 应用实例 ID
     * @return 单条记录，如果存不在则返回 null
     */
    RtAppInstanceDTO get(String appInstanceId);

    /**
     * 获取指定 componentInstanceId 对应的组件实例状态详情
     *
     * @param appInstanceId       应用实例 ID
     * @param componentInstanceId 组件实例 ID
     * @return 单条记录，如果存不在则返回 null
     */
    RtComponentInstanceDTO getComponentInstance(String appInstanceId, String componentInstanceId);

    /**
     * 删除指定的应用实例
     *
     * @param appInstanceId 应用实例 ID
     * @return 删除数量
     */
    int delete(String appInstanceId);
}
