package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleRelMapper {

    List<UserRoleRelDO> findAllByTenantIdAndUserIdAndAppId(
        @Param("locale") String locale,
        @Param("tenantId") String tenantId,
        @Param("userId") String userId,
        @Param("appId") String appId);

    List<UserRoleRelDO> findAllByTenantIdAndUserId(
        @Param("locale") String locale,
        @Param("tenantId") String tenantId,
        @Param("userId") String userId);

    List<UserRoleRelDO> findUserByTenantIdAndRoleId(
        @Param("locale") String locale,
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId);

    List<UserRoleRelDO> findDepIdByTenantIdAndRoleId(
        @Param("locale") String locale,
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId);

    List<UserRoleRelDO> findAllByTenantIdAndRoleId(
        @Param("tenantId") String tenantId,
        @Param("roleId") String roleId);

    int deleteAllByTenantIdAndUserIdAndRoleId(@Param("tenantId") String tenantId,
                                              @Param("userId") String userId,
                                              @Param("roleId") String roleId);

    int deleteAll();

    int insert(UserRoleRelDO rel);

    int replace(UserRoleRelDO rel);
}
