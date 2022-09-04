package com.alibaba.tesla.authproxy.model.repository;

import com.alibaba.tesla.authproxy.model.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserDO, Long>,
    JpaSpecificationExecutor<UserDO> {
}
