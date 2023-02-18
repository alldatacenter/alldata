package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;

import java.util.List;

/**
 * 集群服务表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
public interface ClusterServiceInstanceService extends IService<ClusterServiceInstanceEntity> {

    ClusterServiceInstanceEntity getServiceInstanceByClusterIdAndServiceName(Integer clusterId, String parentName);

    String getServiceConfigByClusterIdAndServiceName(Integer id, String node);

    Result listAll(Integer clusterId);

    Result downloadClientConfig(Integer clusterId, String serviceName);

    Result getServiceRoleType(Integer serviceInstanceId);

    Result configVersionCompare(Integer serviceInstanceId,Integer roleGroupId);

    Result delServiceInstance(Integer serviceInstanceId);

    List<ClusterServiceInstanceEntity> listRunningServiceInstance(Integer clusterId);
}

