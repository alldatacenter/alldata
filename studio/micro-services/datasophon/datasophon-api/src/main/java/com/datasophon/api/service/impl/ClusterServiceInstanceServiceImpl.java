package com.datasophon.api.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.*;
import com.datasophon.dao.entity.*;
import com.datasophon.api.service.*;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.SimpleServiceConfig;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterServiceInstanceMapper;
import org.springframework.transaction.annotation.Transactional;


@Service("clusterServiceInstanceService")
@Transactional
public class ClusterServiceInstanceServiceImpl extends ServiceImpl<ClusterServiceInstanceMapper, ClusterServiceInstanceEntity> implements ClusterServiceInstanceService {

    @Autowired
    private ClusterServiceInstanceMapper serviceInstanceMapper;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterServiceDashboardService dashboardService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterAlertHistoryService alertHistoryService;

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Autowired
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;

    @Override
    public ClusterServiceInstanceEntity getServiceInstanceByClusterIdAndServiceName(Integer clusterId, String serviceName) {
        return this.getOne(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.SERVICE_NAME, serviceName));
    }

    @Override
    public String getServiceConfigByClusterIdAndServiceName(Integer clusterId, String serviceName) {
        return serviceInstanceMapper.getServiceConfigByClusterIdAndServiceName(clusterId, serviceName);
    }

    @Override
    public Result listAll(Integer clusterId) {

        HashMap<String, String> globalVariables = (HashMap<String, String>) CacheUtils.get("globalVariables" + Constants.UNDERLINE + clusterId);
        List<ClusterServiceInstanceEntity> list = this.list(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID, clusterId).orderByAsc(Constants.SORT_NUM));
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        for (ClusterServiceInstanceEntity serviceInstance : list) {
            serviceInstance.setServiceStateCode(serviceInstance.getServiceState().getValue());
            boolean needUpdate = false;
            //查询dashboard
            ClusterServiceDashboard dashboard = dashboardService.getOne(new QueryWrapper<ClusterServiceDashboard>().eq(Constants.SERVICE_NAME, serviceInstance.getServiceName()));
            if (Objects.nonNull(dashboard)) {
                String dashboardUrl = PlaceholderUtils.replacePlaceholders(dashboard.getDashboardUrl(), globalVariables, Constants.REGEX_VARIABLE);
                serviceInstance.setDashboardUrl(dashboardUrl);
            }
            //查询告警数量
            int alertNum = alertHistoryService.count(new QueryWrapper<ClusterAlertHistory>()
                    .eq(Constants.SERVICE_INSTANCE_ID, serviceInstance.getId()).eq(Constants.IS_ENABLED, 1));
            serviceInstance.setAlertNum(alertNum);
            List<ClusterServiceRoleInstanceEntity> totalRoleList = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                    .eq(Constants.SERVICE_ID, serviceInstance.getId()));
            if (Objects.nonNull(totalRoleList) && totalRoleList.size() == 0) {
                serviceInstance.setServiceState(ServiceState.WAIT_INSTALL);
                needUpdate = true;
            }

            //查询停止状态角色
            List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                    .eq(Constants.SERVICE_ID, serviceInstance.getId())
                    .eq(Constants.SERVICE_ROLE_STATE, ServiceRoleState.STOP));
            if (Objects.nonNull(roleList) && roleList.size() > 0) {
                if (!ServiceState.EXISTS_EXCEPTION.equals(serviceInstance.getServiceState())) {
                    serviceInstance.setServiceState(ServiceState.EXISTS_EXCEPTION);
                    needUpdate = true;
                }
            } else {
                if (!ServiceState.RUNNING.equals(serviceInstance.getServiceState())
                        && serviceInstance.getServiceState() != ServiceState.WAIT_INSTALL
                        && serviceInstance.getServiceState() != ServiceState.EXISTS_ALARM) {
                    serviceInstance.setServiceState(ServiceState.RUNNING);
                    needUpdate = true;
                }
            }
            //查询告警状态角色
            List<ClusterServiceRoleInstanceEntity> alarmRoleList = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                    .eq(Constants.SERVICE_ID, serviceInstance.getId())
                    .eq(Constants.SERVICE_ROLE_STATE, ServiceRoleState.EXISTS_ALARM));
            if (Objects.nonNull(alarmRoleList) && alarmRoleList.size() > 0) {
                if (!ServiceState.EXISTS_ALARM.equals(serviceInstance.getServiceState())) {
                    serviceInstance.setServiceState(ServiceState.EXISTS_ALARM);
                    needUpdate = true;
                }
            } else {
                if (serviceInstance.getServiceState() == ServiceState.EXISTS_ALARM) {
                    serviceInstance.setServiceState(ServiceState.RUNNING);
                    needUpdate = true;
                }
            }

            //查询是否进行了配置更新
            List<ClusterServiceRoleInstanceEntity> obsoleteRoleList = roleInstanceService.getObsoleteService(serviceInstance.getId());
            if (Objects.nonNull(obsoleteRoleList) && obsoleteRoleList.size() == 0 && serviceInstance.getNeedRestart() == NeedRestart.YES) {
                serviceInstance.setNeedRestart(NeedRestart.NO);
                needUpdate = true;
            }
            if (needUpdate) {
                this.updateById(serviceInstance);
            }
        }
        return Result.success(list);
    }

    @Override
    public Result downloadClientConfig(Integer clusterId, String serviceName) {

        return null;
    }

    @Override
    public Result getServiceRoleType(Integer serviceInstanceId) {
        ClusterServiceInstanceEntity serviceInstanceEntity = this.getById(serviceInstanceId);
        Integer frameServiceId = serviceInstanceEntity.getFrameServiceId();
        List<FrameServiceRoleEntity> list = frameServiceRoleService.getAllServiceRoleList(frameServiceId);
        return Result.success(list);
    }

    @Override
    public Result configVersionCompare(Integer serviceInstanceId, Integer roleGroupId) {
        List<ClusterServiceRoleGroupConfig> list = roleGroupConfigService.list(new QueryWrapper<ClusterServiceRoleGroupConfig>()
                .eq(Constants.ROLE_GROUP_ID, roleGroupId)
                .orderByDesc(Constants.CONFIG_VERSION).last("limit 2"));
        HashMap<String, List<SimpleServiceConfig>> map = new HashMap<>();
        if (Objects.nonNull(list) && list.size() == 2) {
            ClusterServiceRoleGroupConfig newConfig = list.get(0);
            ClusterServiceRoleGroupConfig oldConfig = list.get(1);
            String newConfigJson = newConfig.getConfigJson();
            List<SimpleServiceConfig> newSimpleServiceConfigs = JSONArray.parseArray(newConfigJson, SimpleServiceConfig.class);

            String oldConfigJson = oldConfig.getConfigJson();
            List<SimpleServiceConfig> oldSimpleServiceConfigs = JSONArray.parseArray(oldConfigJson, SimpleServiceConfig.class);
            map.put("newConfig", newSimpleServiceConfigs);
            map.put("oldConfig", oldSimpleServiceConfigs);

        } else if (list.size() == 1) {
            ClusterServiceRoleGroupConfig newConfig = list.get(0);
            String newConfigJson = newConfig.getConfigJson();
            List<SimpleServiceConfig> newSimpleServiceConfigs = JSONArray.parseArray(newConfigJson, SimpleServiceConfig.class);
            map.put("newConfig", newSimpleServiceConfigs);
            map.put("oldConfig", newSimpleServiceConfigs);
        }
        return Result.success(map);
    }

    @Override
    public Result delServiceInstance(Integer serviceInstanceId) {
        if(hasRunningRoleInstance(serviceInstanceId)){
            return Result.error(Status.EXIT_RUNNING_ROLE_INSTANCE.getMsg());
        }
        List<ClusterServiceInstanceRoleGroup> roleGroups = roleGroupService.listRoleGroupByServiceInstanceId(serviceInstanceId);
        List<Integer> roleGroupIds = roleGroups.stream().map(e -> e.getId()).collect(Collectors.toList());
        List<ClusterServiceRoleGroupConfig> roleGroupConfigList = roleGroupConfigService.listRoleGroupConfigsByRoleGroupIds(roleGroupIds);
        List<ClusterServiceRoleInstanceEntity> roleInstanceList = roleInstanceService.getServiceRoleInstanceListByServiceId(serviceInstanceId);

        //del role group
        roleGroupService.removeByIds(roleGroupIds);
        //del role group config
        roleGroupConfigService.removeByIds(roleGroupConfigList.stream().map(e -> e.getId()).collect(Collectors.toList()));
        //del service role instance
        if(roleInstanceList.size() > 0){
            List<String> roleInsIds = roleInstanceList.stream().map(e -> e.getId().toString()).collect(Collectors.toList());
            roleInstanceService.deleteServiceRole(roleInsIds);
        }
        //del web uis
        webuisService.removeByServiceInsId(serviceInstanceId);

        //del service instance
        this.removeById(serviceInstanceId);
        return Result.success();
    }

    @Override
    public List<ClusterServiceInstanceEntity> listRunningServiceInstance(Integer clusterId) {
        return this.list(new QueryWrapper<ClusterServiceInstanceEntity>()
                .eq(Constants.CLUSTER_ID,clusterId)
                .eq(Constants.SERVICE_STATE,ServiceState.RUNNING));
    }

    private boolean hasRunningRoleInstance(Integer serviceInstanceId) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService.getRunningServiceRoleInstanceListByServiceId(serviceInstanceId);
        if(list.size() > 0){
            return true;
        }
        return false;
    }
}
