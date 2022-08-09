package com.platform.backend.dao;

import com.platform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    User getUserByName(String name);

    User save(User user);
}
