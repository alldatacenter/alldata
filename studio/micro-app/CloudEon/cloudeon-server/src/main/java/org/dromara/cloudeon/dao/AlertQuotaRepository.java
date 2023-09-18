package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ClusterAlertQuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertQuotaRepository extends JpaRepository<ClusterAlertQuotaEntity, Integer> {
}