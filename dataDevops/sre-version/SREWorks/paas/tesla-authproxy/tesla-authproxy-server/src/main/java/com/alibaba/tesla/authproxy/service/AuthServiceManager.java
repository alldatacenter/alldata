package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.model.*;
import com.alibaba.tesla.authproxy.model.vo.ImportPermissionsVO;
import com.alibaba.tesla.authproxy.model.vo.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Title: AuthServiceManager.java<／p>
 * <p>
 * Description: 权限服务管理接口<／p>
 * <p>
 * Copyright: Copyright (c) 2017<／p>
 * <p>
 * Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年4月8日
 */
public interface AuthServiceManager {

    /**
     * 初始化应用下的菜单并授权给该应用下的默认角色
     *
     * @param loginUser 当前登录用户
     * @param appMenus  初始化数据
     * @throws ApplicationException
     */
    void initMenuByAppNew(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException;

    /**
     * 根据 userDo 获取用户扩展信息
     *
     * @param userDo 用户对象
     * @return
     */
    UserExtDO getUserExtInfo(UserDO userDo);

    /**
     * 根据工号从对应系统中获取全量的用户信息
     *
     * @param empId 工号
     * @return
     */
    UserDO getUserByEmpId(String empId);

    /**
     * 查询当前登录用户角色下有权限的菜单数据
     *
     * @param loginuser 当前登录用户
     * @param appId     应用ID
     * @return
     * @throws ApplicationException
     */
    List<MenuDoTreeVO> listMenuByRole(UserDO loginuser, String appId) throws ApplicationException;

    /**
     * 查询某个应用下的所有菜单
     *
     * @param loginUser 当前登录用户
     * @param appId     应用ID
     * @return
     * @throws ApplicationException
     */
    List<MenuDoTreeVO> listMenuByApp(UserDO loginUser, String appId) throws ApplicationException;

    /**
     * 批量添加应用菜单信息
     *
     * @param loginUser 当前登录用户
     * @param appMenus  添加的菜单信息VO
     * @return
     * @throws ApplicationException
     */
    int batchAddMenu(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException;

    /**
     * 验权
     *
     * @param loginUser      当前登录用户ID
     * @param permissionName 权限名称
     * @param appId          应用ID
     * @return true/false
     * @throws ApplicationException
     */
    boolean checkPermission(UserDO loginUser, String permissionName, String appId) throws ApplicationException;

    /**
     * 添加角色信息
     *
     * @param loginUser 前登录用户
     * @param roleVos   角色集合
     * @return key为RoleId value为添加结果
     * @throws ApplicationException
     */
    Map<String, Object> addRoles(UserDO loginUser, List<TeslaRoleVO> roleVos) throws ApplicationException;

    /**
     * 添加权限并授权个某个角色
     *
     * @param loginUser     当前登录用户
     * @param addPermission 添加的权限数据
     * @return
     * @throws ApplicationException
     */
    Map<String, Object> addPermission(UserDO loginUser, AddPermissionVO addPermission) throws ApplicationException;

    /**
     * 删除权限
     *
     * @param userEmployeeId 用户工号
     * @param appId
     * @param permissionName
     * @throws ApplicationException
     */
    boolean delPermission(String userEmployeeId, String appId, String permissionName) throws ApplicationException;

    /**
     * 获取某个应用下的所有角色信息
     *
     * @param loginUser 当前登录用户
     * @param appId     应用ID
     * @return
     * @throws ApplicationException
     */
    List<TeslaRoleVO> getAllRoles(UserDO loginUser, String appId) throws ApplicationException;

    /**
     * 根据用户工号获取用户在ACL或OAM的ID
     *
     * @param empId
     * @return
     */
    String getUserId(String empId);

    /**
     * ============================如下方法暂时没有使用==========================================================
     */

    /**
     * 查询用户的所有权限集合
     *
     * @param loginuser 登录用户
     * @param appId     应用ID
     * @return
     * @throws ApplicationException
     */
    List<UserPermissionsVO> getPermissionsByUserId(UserDO loginuser, String appId) throws ApplicationException;

    /**
     * 查询用户在某个应用下的某类数据有权限的数据集合
     *
     * @param loginuser 当前登录用户
     * @param dataName  某类数据名称
     * @param appId     应用标识
     * @return 该类数据用户有权限的所有数据值
     * @throws ApplicationException
     */
    List<PermissionDO> getDataPermission(UserDO loginuser, String dataName, String appId)
        throws ApplicationException;

    /**
     * 导入权限列表
     *
     * @param user  用户信息
     * @param param 权限列表
     */
    void importPermissions(UserDO user, ImportPermissionsVO param) throws AuthProxyException;

    /**
     * 从 OAM 中同步每个角色绑定的人员，到本地的 user_role_rel 表中
     *
     * @param user       用户
     * @param localRoles 本地当前存在的角色列表
     */
    void syncFromRoleOperator(UserDO user, List<RoleDO> localRoles);

    /**
     * 初始化权限
     *
     * @param userEmployeeId    用户工号
     * @param appId
     * @param permissionMetaDOS
     * @throws AuthProxyException
     */
    void initPermission(String userEmployeeId, String appId, List<PermissionMetaDO> permissionMetaDOS);

    /**
     * 根据权限元数据Code生成ACL或OAM权限ID
     *
     * @param appId
     * @param permissionMetaDO
     * @return
     */
    String createPermissionId(String appId, PermissionMetaDO permissionMetaDO);

    /**
     * 根据应用创建对应的角色名称 主要场景是OAM中没有应用的概念，在应用管理的模式下在权代注册应用之后会根据应用标识在OAM创建对应的角色
     *
     * @param appId
     * @return
     */
    String createRoleNameByAppId(String appId);

}
