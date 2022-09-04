package com.alibaba.tesla.appmanager.server.service.componentpackage.instance;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;

/**
 * @InterfaceName:ComponentPackageBase
 * @Author:dyj
 * @DATE: 2021-03-09
 * @Description:
 **/
public interface ComponentPackageBase {

    /**
     * 导出 component package 到 zip
     *
     * @param taskDO
     */
    void exportComponentPackage(ComponentPackageTaskDO taskDO) throws Exception;
}
