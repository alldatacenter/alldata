package com.alibaba.tesla.gateway.server.repository.domain;

import java.util.List;

/**
 * 专有云和弹内不同的实现
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface RouteInfoRepository {

    /**
     * 获取所有的路由信息
     * @return
     * @throws Exception
     */
    List<RouteInfoDO> listAll() throws Exception;

    /**
     * 保存所有路由配置信息
     * @param routeInfoDOS route infos
     * @return boolean
     * @throws Exception
     */
    boolean saveAll(List<RouteInfoDO> routeInfoDOS) throws Exception;
}
