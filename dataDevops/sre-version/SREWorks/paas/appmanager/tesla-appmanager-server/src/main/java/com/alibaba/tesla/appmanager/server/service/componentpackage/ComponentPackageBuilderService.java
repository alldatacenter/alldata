package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;

import java.io.IOException;

/**
 * Component Package 构建服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentPackageBuilderService {

    /**
     * 构建一个实体 Component Package
     *
     * @param request ComponentPackage 创建任务对象
     * @return 实体包信息
     */
    LaunchBuildComponentHandlerRes build(BuildComponentHandlerReq request) throws IOException;

    /**
     * 利用 kaniko 构建一个 Component Package
     *
     * @param taskDO
     * @return
     * @throws IOException
     */
    void kanikoBuild(ComponentPackageTaskDO taskDO) throws Exception;
}
