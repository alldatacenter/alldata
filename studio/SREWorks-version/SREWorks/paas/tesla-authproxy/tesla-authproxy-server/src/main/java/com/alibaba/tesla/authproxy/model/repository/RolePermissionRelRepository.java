package com.alibaba.tesla.authproxy.model.repository;

import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRelRepository extends JpaRepository<RolePermissionRelDO, Long>,
    JpaSpecificationExecutor<RolePermissionRelDO> {

    List<RolePermissionRelDO> findAllByTenantIdAndRoleIdIn(String tenantId, Collection<String> roleIds);
}
