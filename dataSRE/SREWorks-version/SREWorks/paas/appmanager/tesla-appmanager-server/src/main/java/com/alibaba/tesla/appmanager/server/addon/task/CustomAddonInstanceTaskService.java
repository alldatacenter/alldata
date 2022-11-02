package com.alibaba.tesla.appmanager.server.addon.task;

import com.alibaba.tesla.appmanager.server.addon.req.ApplyCustomAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @InterfaceName:CustomAddonInstanceTaskService
 * @DATE: 2020-11-26
 * @Description:
 **/
public interface CustomAddonInstanceTaskService {
    /**
     * 查询 custom addon instance 任务
     *
     * @param customAddonInstanceTaskId
     * @return
     */
    CustomAddonInstanceTaskDO query(long customAddonInstanceTaskId);

    /**
     * 申请 custom addon instance 任务
     *
     * @param request
     * @return
     */
    CustomAddonInstanceTaskDO apply(ApplyCustomAddonInstanceReq request);
}
