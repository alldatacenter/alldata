package com.alibaba.tesla.authproxy.model.repository;

import com.alibaba.tesla.authproxy.model.RoleDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleRepository extends JpaRepository<RoleDO, Long>,
    JpaSpecificationExecutor<RoleDO> {
}
