package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ServiceInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstanceEntity, Integer> {

    Integer countByClusterId(Integer clusterId);

    @Query(value = "select a.id  from ServiceInstanceEntity a join StackServiceEntity b on a.stackServiceId = b.id where a.clusterId =:clusterId and  b.name = :stackServiceName")
    Integer findByClusterIdAndStackServiceName(@Param("clusterId") Integer clusterId, @Param("stackServiceName") String stackServiceName);

    @Query(value = "select a  from ServiceInstanceEntity a join StackServiceEntity b on a.stackServiceId = b.id where a.clusterId =:clusterId and  b.name = :stackServiceName")
    ServiceInstanceEntity findEntityByClusterIdAndStackServiceName(@Param("clusterId") Integer clusterId, @Param("stackServiceName") String stackServiceName);

    ServiceInstanceEntity findByClusterIdAndStackServiceId(Integer clusterId, Integer stackServiceId);

    List<ServiceInstanceEntity> findByClusterId(Integer clusterId);

    List<ServiceInstanceEntity> findByClusterIdAndDependenceServiceInstanceIdsNotNull(Integer clusterId);


}