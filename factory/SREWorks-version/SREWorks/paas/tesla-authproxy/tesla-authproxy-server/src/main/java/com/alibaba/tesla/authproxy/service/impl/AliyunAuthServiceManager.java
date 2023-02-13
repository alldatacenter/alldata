package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.model.mapper.PermissionResMapper;
import com.alibaba.tesla.authproxy.model.*;
import com.alibaba.tesla.authproxy.model.vo.ImportPermissionsVO;
import com.alibaba.tesla.authproxy.model.vo.*;
import com.alibaba.tesla.authproxy.service.AuthServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云权限管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component("aliyunAuthServiceManager")
@Slf4j
public class AliyunAuthServiceManager implements AuthServiceManager {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private PermissionResMapper permissionResMapper;

    @Override
    public void initMenuByAppNew(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public UserExtDO getUserExtInfo(UserDO userDo) {
        return new UserExtDO();
    }

    @Override
    public UserDO getUserByEmpId(String empId) {
        return null;
    }

    @Override
    public List<MenuDoTreeVO> listMenuByRole(UserDO loginuser, String appId) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public List<MenuDoTreeVO> listMenuByApp(UserDO loginUser, String appId) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public int batchAddMenu(UserDO loginUser, AppMenuVO appMenus) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public boolean checkPermission(UserDO loginUser, String permissionName, String appId) throws ApplicationException {
        return true;
    }

    @Override
    public Map<String, Object> addRoles(UserDO loginUser, List<TeslaRoleVO> roleVos) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Object> addPermission(UserDO loginUser, AddPermissionVO addPermission) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public boolean delPermission(String userEmployeeId, String appId, String permissionName) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public List<TeslaRoleVO> getAllRoles(UserDO loginUser, String appId) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public String getUserId(String empId) {
        throw new NotImplementedException();
    }

    @Override
    public List<UserPermissionsVO> getPermissionsByUserId(UserDO loginuser, String appId)
        throws ApplicationException {
        List<UserPermissionsVO> ret = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        for (PermissionResDO item : permissionResMapper.query(params)) {
            UserPermissionsVO userPermission = new UserPermissionsVO();
            userPermission.setPermissionName(item.getPermissionId());
            userPermission.setAccessible(true);
            userPermission.setReqPath(item.getResPath());
            userPermission.setApplyLink("http://fakeurl");
            userPermission.setName(item.getMemo());
            ret.add(userPermission);
        }
        return ret;
    }

    @Override
    public List<PermissionDO> getDataPermission(UserDO loginuser, String dataName, String appId) throws ApplicationException {
        throw new NotImplementedException();
    }

    @Override
    public void importPermissions(UserDO user, ImportPermissionsVO param) throws AuthProxyException {}

    /**
     * 从 OAM 中同步每个角色绑定的人员，到本地的 user_role_rel 表中
     *
     * @param localRoles 角色 ID
     */
    @Override
    public void syncFromRoleOperator(UserDO user, List<RoleDO> localRoles) {

    }

    @Override
    public void initPermission(String userEmployeeId, String appId, List<PermissionMetaDO> permissionMetaDOS) {}

    @Override
    public String createPermissionId(String appId, PermissionMetaDO permissionMetaDO) {
        return authProperties.getPermissionPrefix() + appId + "_" + permissionMetaDO.getPermissionCode();
    }

    @Override
    public String createRoleNameByAppId(String appId) {
        return authProperties.getOamAdminRole() + "_appmgr_" + appId;
    }
}
