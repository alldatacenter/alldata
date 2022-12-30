package com.alibaba.tesla.appmanager.server.service.appmarket;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMarketQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;

/**
 * 应用市场服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppMarketService {

    /**
     * 根据条件过滤应用包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<AppPackageDO> list(AppMarketQueryCondition condition);
}
