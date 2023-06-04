package org.dromara.cloudeon.dao;

import org.dromara.cloudeon.entity.StackServiceRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StackServiceRoleRepository extends JpaRepository<StackServiceRoleEntity, Integer> {

    public StackServiceRoleEntity findByStackIdAndNameAndServiceId(Integer stackId, String name, Integer serviceId);

    public List<StackServiceRoleEntity> findByServiceIdAndStackId(Integer serviceId, Integer stackId);

    public List<StackServiceRoleEntity> findByServiceIdOrderBySortNum(Integer serviceId);

    StackServiceRoleEntity findByServiceIdAndName(Integer serviceId, String name);
}