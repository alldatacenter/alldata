package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.req.ReleaseAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonInstanceRes;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;

/**
 * Addon 资源实例管理统一接口
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface AddonInstanceManager {

    /**
     * 根据指定条件查询唯一的 addon instance
     *
     * @param condition 查询条件
     * @return addon instance 对象
     */
    AddonInstanceDO getByCondition(AddonInstanceQueryCondition condition);

    /**
     * 资源申请
     *
     * @param request 资源申请请求
     */
    ApplyAddonInstanceRes applyAddonInstance(ApplyAddonInstanceReq request);

    /**
     * 释放资源
     *
     * @param request 释放资源请求
     */
    String releaseAddonInstance(ReleaseAddonInstanceReq request);
}
