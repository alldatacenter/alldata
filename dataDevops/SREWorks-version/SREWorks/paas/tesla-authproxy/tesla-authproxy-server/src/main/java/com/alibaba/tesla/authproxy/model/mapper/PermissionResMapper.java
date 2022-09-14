package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.PermissionResDO;

import java.util.List;
import java.util.Map;

/**
 * <p>Title: PermissionResMapper.java<／p>
 * <p>Description: 权限资源数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface PermissionResMapper {

    /**
     * 根据资源路径查询 左边包含path
     *
     * @param params
     * @return
     */
    PermissionResDO getByAppAndResPath(Map<String, Object> params);

    /**
     * 根据appId和permissionId查询权限资源表
     * @param appId
     * @param permissionId
     * @return
     */
    PermissionResDO getByAppIdAndPermissionId(String appId, String permissionId);

    /**
     * 根据资源路径查询，path完全相等
     *
     * @param params
     * @return
     */
    PermissionResDO getByAppAndPath(Map<String, Object> params);

    /**
     * 查询唯一的权限资源
     * @param appId
     * @return
     */
    PermissionResDO selectOne(String appId, String permissionId);

    /**
     * 查询某个App配置的所有acl权限点名称
     *
     * @param appId
     * @return
     */
    List<String> queryPermissionsByAppId(String appId);

    /**
     * 统计某个app 某个服务下授权的权限数量
     * @param appId
     * @param serviceCode
     * @return
     */
    int countByAppIdAndServiceCode(String appId, String serviceCode);

    /**
     * 根据参数查询列表
     *
     * @param params
     * @return
     */
    List<PermissionResDO> query(Map<String, Object> params);

    /**
     * 添加权限资源关系
     *
     * @param permissionResDo
     * @return
     */
    int insert(PermissionResDO permissionResDo);

    /**
     * 删除权限资源
     *
     * @param id
     * @return
     */
    int delete(long id);

    /**
     * 根据应用ID和权限ID删除
     * @param appId
     * @param permissionId
     * @return
     */
    int deleteByAppIdAndPermissionId(String appId, String permissionId);

    /**
     * 更新权限资源关系
     *
     * @param permissionResDo
     * @return
     */
    int updateByPrimaryKey(PermissionResDO permissionResDo);

    /**
     * 更新不为空权限资源
     * @param permissionResDo
     * @return
     */
    int updateByPrimaryKeySelective(PermissionResDO permissionResDo);

    /**
     * 批量插入权限资源对应关系
     *
     * @param permissionResDOS
     * @return
     */
    int batchInsert(List<PermissionResDO> permissionResDOS);
}