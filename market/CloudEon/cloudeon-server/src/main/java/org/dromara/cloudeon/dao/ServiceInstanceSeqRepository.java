package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.ServiceInstanceSeqEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInstanceSeqRepository extends JpaRepository<ServiceInstanceSeqEntity, Integer> {
    ServiceInstanceSeqEntity findByStackServiceId(Integer stackServiceId);

}