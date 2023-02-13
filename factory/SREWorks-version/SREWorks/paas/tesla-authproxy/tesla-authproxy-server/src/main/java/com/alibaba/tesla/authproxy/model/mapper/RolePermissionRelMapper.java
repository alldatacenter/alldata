package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.RoleDO;
import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionRelMapper {

    List<RolePermissionRelDO> findAllByRoleId(
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId);

    List<RolePermissionRelDO> findAllByUserIdAndRoleIdPrefix(
        @Param("tenantId") String tenantId,
        @Param("userId") String userId,
        @Param("roleIdPrefix") String roleIdPrefix);

    List<RoleDO> findAllByResourcePath(
        @Param("tenantId") String tenantId,
        @Param("resourcePath") String resourcePath);

    RolePermissionRelDO get(
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId,
        @Param("resourcePath") String resourcePath);

    int delete(
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId,
        @Param("resourcePath") String resourcePath);

    int deleteAll();

    int insert(RolePermissionRelDO rolePermissionRelDO);
}
