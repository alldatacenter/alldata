package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;

/**
 * 集群服务角色实例配置表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
public interface ClusterServiceInstanceConfigService extends IService<ClusterServiceInstanceConfigEntity> {

    Result getServiceInstanceConfig(Integer serviceInstanceId, Integer version, Integer roleGroupId, Integer page, Integer pageSize);

    ClusterServiceInstanceConfigEntity getServiceConfigByServiceId(Integer id);

    Result getConfigVersion(Integer serviceInstanceId,Integer roleGroupId);
}

