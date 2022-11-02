package com.alibaba.tesla.appmanager.server.addon.task;

import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;

/**
 * 附加组件服务接口
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface AddonInstanceTaskService {

    /**
     * 查询指定 addonInstanceTaskId 对应的任务运行对象
     *
     * @param addonInstanceTaskId Addon Instance 任务 ID
     * @return AddonInstanceTaskDO
     */
    AddonInstanceTaskDO query(long addonInstanceTaskId);

    /**
     * 申请 Addon Instance
     *
     * @param request 申请请求
     * @return addonInstanceTaskId
     */
    AddonInstanceTaskDO apply(ApplyAddonInstanceReq request);
}
