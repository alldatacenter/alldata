package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.AlertMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertMessageRepository extends JpaRepository<AlertMessageEntity, Integer> {
    AlertMessageEntity findByFireTimeAndAlertNameAndHostname(String fireTime, String alertName, String hostname);

    @Query(value = "select a  from AlertMessageEntity a  where  a.resolved =:resolved and a.clusterId=:clusterId")
    List<AlertMessageEntity> findByIsResolve(@Param("resolved")Boolean resolved,@Param("clusterId")Integer clusterId);

    List<AlertMessageEntity> findByServiceInstanceIdAndResolved(Integer serviceInstanceId, boolean resolved);
    List<AlertMessageEntity> findByServiceRoleInstanceIdAndResolved(Integer roleInstanceId, boolean resolved);
}