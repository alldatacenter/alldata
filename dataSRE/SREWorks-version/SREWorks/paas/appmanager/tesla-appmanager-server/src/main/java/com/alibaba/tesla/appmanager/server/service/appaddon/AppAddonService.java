package com.alibaba.tesla.appmanager.server.service.appaddon;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.AppAddonCreateReq;
import com.alibaba.tesla.appmanager.domain.req.appaddon.AppAddonSyncReq;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;

/**
 * 应用组件服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppAddonService {

    /**
     * 根据条件过滤应用组件
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<AppAddonDO> list(AppAddonQueryCondition condition);

    /**
     * 根据条件查询某个应用组件
     *
     * @param condition 查询条件
     * @return AppAddonDO
     */
    AppAddonDO get(AppAddonQueryCondition condition);

    /**
     * 更新指定的应用组件
     *
     * @param record App Addon 记录
     */
    int update(AppAddonDO record, AppAddonQueryCondition condition);

    /**
     * 更新指定的应用组件
     *
     * @param request 创建请求
     */
    AppAddonDO create(AppAddonCreateReq request);

    /**
     * 根据条件删除应用组件
     *
     * @param condition 查询条件
     */
    int delete(AppAddonQueryCondition condition);

    /**
     * 同步当前所有 app addon 绑定关系到 deploy config 中
     */
    void sync(AppAddonSyncReq request);
}
