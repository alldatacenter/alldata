package com.datasophon.api.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceInstanceConfigMapper;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.datasophon.api.service.ClusterServiceInstanceConfigService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service("clusterServiceInstanceConfigService")
public class ClusterServiceInstanceConfigServiceImpl extends ServiceImpl<ClusterServiceInstanceConfigMapper, ClusterServiceInstanceConfigEntity> implements ClusterServiceInstanceConfigService {

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Override
    public Result getServiceInstanceConfig(Integer serviceInstanceId, Integer version, Integer roleGroupId, Integer page, Integer pageSize) {
        ClusterServiceRoleGroupConfig roleGroupConfig = roleGroupConfigService.getConfigByRoleGroupIdAndVersion(roleGroupId,version);
        if (Objects.nonNull(roleGroupConfig) ) {
            String configJson = roleGroupConfig.getConfigJson();
            List<ServiceConfig> serviceConfigs = JSONObject.parseArray(configJson, ServiceConfig.class);
            return Result.success(serviceConfigs);
        }
        return Result.success();
    }

    @Override
    public ClusterServiceInstanceConfigEntity getServiceConfigByServiceId(Integer id) {
        return this.getOne(new QueryWrapper<ClusterServiceInstanceConfigEntity>()
                .eq(Constants.SERVICE_ID, id)
                .orderByDesc(Constants.CONFIG_VERSION).last("limit 1"));
    }

    @Override
    public Result getConfigVersion(Integer serviceInstanceId,Integer roleGroupId) {

        List<ClusterServiceRoleGroupConfig> list = roleGroupConfigService.list(new QueryWrapper<ClusterServiceRoleGroupConfig>()
                .eq(Constants.ROLE_GROUP_ID, roleGroupId)
                .orderByDesc(Constants.CONFIG_VERSION));
        List<Integer> versions = list.stream().map(e -> e.getConfigVersion()).collect(Collectors.toList());
        return Result.success(versions);
    }
}
