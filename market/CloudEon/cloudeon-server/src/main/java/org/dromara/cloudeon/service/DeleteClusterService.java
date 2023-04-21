package org.dromara.cloudeon.service;

import org.dromara.cloudeon.dao.ServiceInstanceConfigRepository;
import org.dromara.cloudeon.dao.ServiceInstanceRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceRepository;
import org.dromara.cloudeon.dao.ServiceRoleInstanceWebuisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class DeleteClusterService {
    @Resource
    ServiceInstanceRepository serviceInstanceRepository ;
    @Resource
    ServiceInstanceConfigRepository serviceInstanceConfigRepository;
    @Resource
    ServiceRoleInstanceRepository roleInstanceRepository ;
    @Resource
    ServiceRoleInstanceWebuisRepository roleInstanceWebuisRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deleteOneService(Integer serviceInstanceId) {
        // 删除服务实例表
        serviceInstanceRepository.deleteById(serviceInstanceId);
        // 删除服务角色实例表
        roleInstanceRepository.deleteByServiceInstanceId(serviceInstanceId);
        // 删除服务角色配置表
        serviceInstanceConfigRepository.deleteByServiceInstanceId(serviceInstanceId);
        // 删除服务ui表
        roleInstanceWebuisRepository.deleteByServiceInstanceId(serviceInstanceId);
    }
}
