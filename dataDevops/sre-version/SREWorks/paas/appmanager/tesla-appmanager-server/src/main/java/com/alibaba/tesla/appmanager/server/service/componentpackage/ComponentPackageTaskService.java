package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageTaskNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;

/**
 * 组件包任务服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentPackageTaskService {

    /**
     * 根据条件过滤组件包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<ComponentPackageTaskDO> list(ComponentPackageTaskQueryCondition condition);

    /**
     * 根据条件获取单个组件包
     *
     * @param condition 过滤条件
     * @return List
     */
    ComponentPackageTaskDO get(ComponentPackageTaskQueryCondition condition);

    /**
     * 更新组件构建任务包
     *
     * @param record    任务对象
     * @param condition 查询条件
     * @return 更新数量
     */
    int update(ComponentPackageTaskDO record, ComponentPackageTaskQueryCondition condition);

    /**
     * 根据查询条件删除任务记录
     *
     * @param condition 查询条件
     * @return 删除数量
     */
    int delete(ComponentPackageTaskQueryCondition condition);

    /**
     * 获取指定组件包任务的下一个可用版本 (with build number)
     *
     * @param req 查询请求
     * @return 下一个可用版本 (含当前)
     */
    ComponentPackageTaskNextVersionRes nextVersion(ComponentPackageTaskNextVersionReq req);
}
