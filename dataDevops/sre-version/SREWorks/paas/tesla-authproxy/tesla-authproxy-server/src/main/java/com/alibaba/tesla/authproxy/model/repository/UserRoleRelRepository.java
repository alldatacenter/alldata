package com.alibaba.tesla.authproxy.model.repository;

import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserRoleRelRepository extends JpaRepository<UserRoleRelDO, Long>,
    JpaSpecificationExecutor<UserRoleRelDO> {

    List<UserRoleRelDO> findAllByTenantIdAndUserIdAndRoleIdLike(String tenantId, String userId, String roleId);
}
