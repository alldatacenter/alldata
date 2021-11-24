package com.platform.mall.service.mobile;

import com.platform.mall.entity.mobile.LitemallPermission;

import java.util.Set;

public interface LitemallPermissionService {

    Set<String> queryByRoleIds(Integer[] roleIds);


    Set<String> queryByRoleId(Integer roleId);

    boolean checkSuperPermission(Integer roleId);

    void deleteByRoleId(Integer roleId);

    void add(LitemallPermission litemallPermission);
}
