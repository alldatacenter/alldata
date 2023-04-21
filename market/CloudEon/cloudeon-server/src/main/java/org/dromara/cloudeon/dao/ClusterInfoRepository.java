package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ClusterInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterInfoRepository extends JpaRepository<ClusterInfoEntity, Integer> {
}