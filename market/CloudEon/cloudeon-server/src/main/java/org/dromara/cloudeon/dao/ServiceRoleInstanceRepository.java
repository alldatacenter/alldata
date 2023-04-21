package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ServiceRoleInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRoleInstanceRepository extends JpaRepository<ServiceRoleInstanceEntity, Integer> {
    List<ServiceRoleInstanceEntity> findByServiceInstanceIdAndStackServiceRoleId(Integer serviceInstanceId, Integer stackServiceRoleId);
    List<ServiceRoleInstanceEntity> findByServiceInstanceIdAndServiceRoleName(Integer serviceInstanceId, String serviceRoleName);
    List<ServiceRoleInstanceEntity> findByServiceInstanceId(Integer serviceInstanceId);

    ServiceRoleInstanceEntity findByServiceInstanceIdAndNodeIdAndServiceRoleName(Integer serviceInstanceId, Integer nodeId, String serviceRoleName);

    int deleteByServiceInstanceId(Integer serviceInstanceId);

    int countByServiceInstanceIdAndServiceRoleName(Integer serviceInstanceId, String serviceRoleName);

    @Query(value = "select a  from ServiceRoleInstanceEntity a join ClusterNodeEntity b on a.nodeId = b.id where a.clusterId =:clusterId and  a.serviceRoleName = :roleName and b.hostname =:hostname")
    ServiceRoleInstanceEntity findByServiceRoleNameAndClusterIdAndHostname(@Param("clusterId") Integer clusterId, @Param("roleName") String roleName, @Param("hostname")  String hostname);

    @Query(value = "select b.label  from ServiceRoleInstanceEntity a join StackServiceRoleEntity b on a.stackServiceRoleId = b.id where  a.id =:roleInstanceId")
    String getRoleInstanceLabel(@Param("roleInstanceId") Integer roleInstanceId);
}