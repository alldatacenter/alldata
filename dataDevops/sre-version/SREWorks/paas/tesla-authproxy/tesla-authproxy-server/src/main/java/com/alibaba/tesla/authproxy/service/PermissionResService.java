package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.PermissionResDO;

import java.util.List;

/**
 * <p>Description: 权限资源服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface PermissionResService {

    /**
     * 根据资源路径查询，包含关系，只要是resPath开头都能查询到
     *
     * @param appId   应用ID
     * @param resPath 资源路径
     * @return 权限资源VO
     * @throws ApplicationException
     */
    PermissionResDO getByResPath(String appId, String resPath) throws ApplicationException;

    /**
     * 根据appId 和 path获取唯一的权限资源，path完全相等
     *
     * @param appId   应用标识
     * @param resPath 资源path
     * @return
     * @throws ApplicationException
     */
    PermissionResDO getByAppAndPath(String appId, String resPath) throws ApplicationException;

    /**
     * 添加权限资源关系
     *
     * @param permissionResDo
     * @return
     * @throws ApplicationException
     */
    int insert(PermissionResDO permissionResDo) throws ApplicationException;

    /**
     * 更新
     *
     * @param permissionResDo
     * @return
     * @throws ApplicationException
     */
    int update(PermissionResDO permissionResDo) throws ApplicationException;

    /**
     * 删除
     */
    int delete(long id) throws ApplicationException;


    /**
     * 批量添加权限资源关系
     *
     * @param permissionResDOS
     * @return
     * @throws ApplicationException
     */
    int batchInsert(List<PermissionResDO> permissionResDOS) throws ApplicationException;

    /**
     * 初始化应用权限资源关系列表
     * @param userId
     * @param appId
     * @param accessKey
     * @return
     */
    void init(String userId, String appId, String accessKey) throws ApplicationException;

    /**
     * 开启服务权限
     * @param userEmployeeId 用户工号
     * @param appId 应用标识
     * @param serviceCode 服务标识
     * @param permissionMetaIds 为空则开启该服务下的所有权限
     * @throws ApplicationException
     */
    void enable(String userEmployeeId, String appId, String serviceCode, List<Long> permissionMetaIds) throws ApplicationException;

    /**
     * 关闭服务权限
     * @param userEmployeeId 用户工号
     * @param appId
     * @param serviceCode
     * @param permissionMetaIds 为空则关闭该服务下的所有权限
     * @throws ApplicationException
     */
    void disable(String userEmployeeId, String appId, String serviceCode, List<Long> permissionMetaIds) throws ApplicationException;
}