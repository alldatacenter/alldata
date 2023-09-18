package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, Integer> {
}