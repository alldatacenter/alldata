package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ClusterAlertRuleEntity;
import org.dromara.cloudeon.entity.StackAlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterAlertRuleRepository extends JpaRepository<ClusterAlertRuleEntity, Integer> {

    List<ClusterAlertRuleEntity> findByClusterId(Integer clusterId);
}