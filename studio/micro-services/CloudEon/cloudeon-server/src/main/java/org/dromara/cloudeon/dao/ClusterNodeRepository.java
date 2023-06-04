package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ClusterNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ClusterNodeRepository extends JpaRepository<ClusterNodeEntity, Integer> {
    Integer countByIp(String ip);

    @Modifying
    void deleteByClusterId(Integer clusterId);

    Integer countByClusterId(Integer clusterId);

    ClusterNodeEntity findByHostname(String hostname);

    public List<ClusterNodeEntity> findByClusterId(Integer clusterId);
}