package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceDTO;
import com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance.RtComponentInstanceQueryReq;

/**
 * 实时组件实例 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface RtComponentInstanceProvider {

    /**
     * 根据条件查询实时组件实例
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<RtComponentInstanceDTO> queryByCondition(RtComponentInstanceQueryReq request);
}
