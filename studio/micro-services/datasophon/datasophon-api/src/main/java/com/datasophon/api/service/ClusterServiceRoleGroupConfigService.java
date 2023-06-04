package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;

import java.util.List;

/**
 * 
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-08-16 16:56:01
 */
public interface ClusterServiceRoleGroupConfigService extends IService<ClusterServiceRoleGroupConfig> {


    ClusterServiceRoleGroupConfig getConfigByRoleGroupId(Integer roleGroupId);

    ClusterServiceRoleGroupConfig getConfigByRoleGroupIdAndVersion(Integer roleGroupId, Integer version);

    void removeAllByRoleGroupId(Integer roleGroupId);

    List<ClusterServiceRoleGroupConfig> listRoleGroupConfigsByRoleGroupIds(List<Integer> roleGroupIds);
}

