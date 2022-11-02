package com.alibaba.tesla.appmanager.server.service.groovy;

import com.alibaba.tesla.appmanager.domain.req.groovy.GroovyUpgradeReq;

/**
 * Groovy 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface GroovyService {

    /**
     * 升级指定 code 到系统中加载
     *
     * @param req 脚本升级请求
     */
    void upgradeScript(GroovyUpgradeReq req);
}
