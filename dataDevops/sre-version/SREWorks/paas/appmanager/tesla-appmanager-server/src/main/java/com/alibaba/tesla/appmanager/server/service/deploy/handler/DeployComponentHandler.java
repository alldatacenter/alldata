package com.alibaba.tesla.appmanager.server.service.deploy.handler;

import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes;
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * 部署组件 Handler 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployComponentHandler extends GroovyHandler {

    /**
     * 启动部署组件过程
     *
     * @param request 部署请求
     */
    LaunchDeployComponentHandlerRes launch(LaunchDeployComponentHandlerReq request);

    /**
     * 查询部署组件结果
     *
     * @param request 部署请求
     * @return 查询结果
     */
    GetDeployComponentHandlerRes get(GetDeployComponentHandlerReq request);
}
