package com.alibaba.tesla.appmanager.server.dynamicscript.handler;

import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * 组件构建 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentBuildHandler extends GroovyHandler {

    /**
     * 构建一个实体 Component Package
     *
     * @param request ComponentPackage 创建任务对象
     * @return 实体包信息
     */
    LaunchBuildComponentHandlerRes launch(BuildComponentHandlerReq request);
}
