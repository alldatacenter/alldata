package com.alibaba.tesla.appmanager.server.service.appmeta;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;

import java.util.List;

/**
 * 应用元信息服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface AppMetaService {

    /**
     * 根据条件过滤应用元信息
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<AppMetaDO> list(AppMetaQueryCondition condition);

    /**
     * 根据条件查询某个应用元信息
     *
     * @param condition 查询条件
     * @return AppMetaDO
     */
    AppMetaDO get(AppMetaQueryCondition condition);

    /**
     * 更新指定的应用元信息
     *
     * @param record App Meta 记录
     */
    int update(AppMetaDO record, AppMetaQueryCondition condition);

    /**
     * 更新指定的应用元信息
     *
     * @param record App Meta 记录
     */
    int create(AppMetaDO record);

    /**
     * 根据条件删除应用元信息
     *
     * @param condition 查询条件
     */
    int delete(AppMetaQueryCondition condition);

    /**
     * 获取指定 operator 用户有权限的所有应用 ID 列表
     *
     * @param operator 用户
     * @return List of AppID
     */
    List<String> listUserPermittedApp(String operator);
}
