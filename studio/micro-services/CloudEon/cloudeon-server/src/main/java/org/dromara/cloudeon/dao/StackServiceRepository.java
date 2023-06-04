package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.StackServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StackServiceRepository extends JpaRepository<StackServiceEntity, Integer> {
    public StackServiceEntity findByStackIdAndName(Integer stackId, String name);

    List<StackServiceEntity> findByStackId(Integer stackId);
}