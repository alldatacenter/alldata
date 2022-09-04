package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageCreateByLocalFileReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageNextVersionReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageNextVersionRes;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;

import java.util.List;

/**
 * 内部 - 组件包服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentPackageService {

    /**
     * 根据条件过滤组件包列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<ComponentPackageDO> list(ComponentPackageQueryCondition condition);

    /**
     * 根据条件获取单个组件包列表
     *
     * @param condition 过滤条件
     * @return ComponentPackageDO 对象
     */
    ComponentPackageDO get(ComponentPackageQueryCondition condition);

    /**
     * 添加一个 component package 到当前已经存在的 app package 中
     *
     * @param appPackageId           app package id
     * @param componentPackageIdList component package ID 列表
     */
    void addComponentPackageRelation(String appId, long appPackageId, List<Long> componentPackageIdList);

    /**
     * 创建一个 Component Package
     *
     * @param req 创建请求
     * @return 创建后的 ComponentPackageDO 对象
     */
    ComponentPackageDO createByLocalFile(ComponentPackageCreateByLocalFileReq req);

    /**
     * 根据条件删除包应用
     *
     * @param condition 查询条件
     */
    int delete(ComponentPackageQueryCondition condition);

    /**
     * 获取指定组件包的下一个可用版本 (with build number)
     *
     * @param req 查询请求
     * @return 下一个可用版本 (含当前)
     */
    ComponentPackageNextVersionRes nextVersion(ComponentPackageNextVersionReq req);
}
