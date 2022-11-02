package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.tesla.appmanager.server.addon.req.ApplyCustomAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyCustomAddonInstanceRes;
import com.alibaba.tesla.appmanager.server.repository.condition.CustomAddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @InterfaceName:CustomAddonInstanceManager
 * @DATE: 2020-11-26
 * @Description:
 **/
public interface CustomAddonInstanceManager {
    /**
     * 根据指定条件查询唯一的 custom addon instance
     *
     * @param condition
     * @return
     */
    CustomAddonInstanceDO getByCondition(CustomAddonInstanceQueryCondition condition);

    /**
     * custom addon 资源申请
     *
     * @param request
     * @return
     */
    ApplyCustomAddonInstanceRes applyCustomAddonInstance(ApplyCustomAddonInstanceReq request);
}
