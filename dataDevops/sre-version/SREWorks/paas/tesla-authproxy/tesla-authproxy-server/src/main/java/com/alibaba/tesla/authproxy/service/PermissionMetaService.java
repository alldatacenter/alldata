package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import com.github.pagehelper.PageInfo;

/**
 * <p>Description: 权限元数据服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2018年11月13日
 */
public interface PermissionMetaService {

    /**
     * 创建并初始化应用的权限元数据
     * @param userId
     * @param appId
     * @param permissionMetaDO
     */
    void createAndInit(String userId, String appId, PermissionMetaDO permissionMetaDO);

    /**
     * 查询唯一的权限元数据
     * @param permissionCode
     * @return
     */
    PermissionMetaDO selectOne(String permissionCode);

    /**
     * 分页查询权限元数据
     * @param appId
     * @param serviceCode
     * @param page
     * @param limit
     * @return
     */
    PageInfo<PermissionMetaDO> select(String appId, String serviceCode, int page, int limit);

    /**
     * 根据主键更新不为空的字段
     * @param permissionMetaDO
     * @return
     */
    int update(PermissionMetaDO permissionMetaDO);

    /**
     * 添加权限元数据，只插入不为空字段
     * @param permissionMetaDO
     * @return
     */
    int insert(PermissionMetaDO permissionMetaDO);

    /**
     * 根据主键删除权限元数据
     * @param appId
     * @param userId
     * @param permissionCode
     * @param delAclOam 是否同步删除acl或oam中的权限数据
     * @return
     */
    void delete(String appId, String userId, String permissionCode, boolean delAclOam);
}
