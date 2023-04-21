package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.CommandTaskGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandTaskGroupRepository extends JpaRepository<CommandTaskGroupEntity, Integer> {
}