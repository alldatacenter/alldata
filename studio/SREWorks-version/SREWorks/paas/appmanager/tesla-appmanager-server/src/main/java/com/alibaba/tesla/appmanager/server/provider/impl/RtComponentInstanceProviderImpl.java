package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.RtComponentInstanceProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceDTO;
import com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance.RtComponentInstanceQueryReq;
import com.alibaba.tesla.appmanager.server.assembly.RtComponentInstanceDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 实时组件实例 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class RtComponentInstanceProviderImpl implements RtComponentInstanceProvider {

    @Autowired
    private RtComponentInstanceService rtComponentInstanceService;

    @Autowired
    private RtComponentInstanceDtoConvert rtComponentInstanceConvert;

    /**
     * 根据条件查询实时组件实例
     *
     * @param request 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<RtComponentInstanceDTO> queryByCondition(RtComponentInstanceQueryReq request) {
        RtComponentInstanceQueryCondition condition = new RtComponentInstanceQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<RtComponentInstanceDO> results = rtComponentInstanceService.list(condition);
        return Pagination.transform(results, item -> rtComponentInstanceConvert.to(item));
    }
}
