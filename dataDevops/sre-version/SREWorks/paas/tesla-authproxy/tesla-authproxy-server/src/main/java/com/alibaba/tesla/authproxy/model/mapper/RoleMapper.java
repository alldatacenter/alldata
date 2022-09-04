package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper {

    RoleDO findFirstByTenantIdAndRoleIdAndLocale(@Param("tenantId") String tenantId,
                                                 @Param("roleId") String roleId,
                                                 @Param("locale") String locale);

    List<RoleDO> findAllByTenantIdAndRoleId(@Param("tenantId") String tenantId,
                                            @Param("roleId") String roleId);

    List<RoleDO> findAllByTenantIdAndLocale(@Param("tenantId") String tenantId,
                                            @Param("locale") String locale);

    int deleteAllByTenantIdAndRoleId(@Param("tenantId") String tenantId,
                                     @Param("roleId") String roleId);

    int deleteAll();

    int insert(RoleDO role);

    int update(RoleDO roleDO);
}
