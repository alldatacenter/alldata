package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.RackActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.common.command.GenerateRackPropCommand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.ClusterHostMapper;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.api.service.ClusterHostService;
import org.springframework.transaction.annotation.Transactional;


@Service("clusterHostService")
@Transactional
public class ClusterHostServiceImpl extends ServiceImpl<ClusterHostMapper, ClusterHostEntity> implements ClusterHostService {

    @Autowired
    ClusterHostMapper hostMapper;

    @Autowired
    ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    ClusterInfoService clusterInfoService;

    private final String IP = "ip";

    @Override
    public ClusterHostEntity getClusterHostByHostname(String hostname) {
        return hostMapper.getClusterHostByHostname(hostname);
    }

    @Override
    public Result listByPage(Integer clusterId, String hostname, String ip, String cpuArchitecture, Integer hostState, String orderField, String orderType, Integer page, Integer pageSize) {
        Integer offset = (page - 1) * pageSize;
        List<ClusterHostEntity> list = this.list(new QueryWrapper<ClusterHostEntity>().eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.MANAGED, 1)
                .eq(StringUtils.isNotBlank(cpuArchitecture), Constants.CPU_ARCHITECTURE, cpuArchitecture)
                .eq(hostState != null, Constants.HOST_STATE, hostState)
                .like(StringUtils.isNotBlank(ip), IP, ip)
                .like(StringUtils.isNotBlank(hostname), Constants.HOSTNAME, hostname)
                .orderByAsc("asc".equals(orderType), orderField)
                .orderByDesc("desc".equals(orderType), orderField)
                .last("limit " + offset + "," + pageSize));
        for (ClusterHostEntity clusterHostEntity : list) {
            //查询主机上服务角色数
            int serviceRoleNum = roleInstanceService.count(new QueryWrapper<ClusterServiceRoleInstanceEntity>().eq(Constants.HOSTNAME, clusterHostEntity.getHostname()));
            clusterHostEntity.setServiceRoleNum(serviceRoleNum);
        }
        int count = this.count(new QueryWrapper<ClusterHostEntity>().eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.MANAGED, 1)
                .eq(StringUtils.isNotBlank(cpuArchitecture), Constants.CPU_ARCHITECTURE, cpuArchitecture)
                .eq(hostState != null, Constants.HOST_STATE, hostState)
                .like(StringUtils.isNotBlank(hostname), Constants.HOSTNAME, hostname));
        return Result.success(list).put(Constants.TOTAL, count);
    }

    @Override
    public List<ClusterHostEntity> getHostListByClusterId(Integer clusterId) {
        return this.list(new QueryWrapper<ClusterHostEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.MANAGED, 1));
    }

    @Override
    public Result getRoleListByHostname(Integer clusterId, String hostname) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService.getServiceRoleListByHostnameAndClusterId(hostname, clusterId);
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : list) {
            roleInstanceEntity.setServiceRoleStateCode(roleInstanceEntity.getServiceRoleState().getValue());
        }
        return Result.success(list);
    }

    @Override
    public Result deleteHost(Integer hostId) {
        ClusterHostEntity host = this.getById(hostId);
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService.list(new QueryWrapper<ClusterServiceRoleInstanceEntity>()
                .eq(Constants.CLUSTER_ID, host.getClusterId())
                .eq(Constants.HOSTNAME, host.getHostname())
                .eq(Constants.SERVICE_ROLE_STATE, ServiceRoleState.RUNNING)
                .ne(Constants.ROLE_TYPE, RoleType.CLIENT));
        List<String> roles = list.stream().map(e -> e.getServiceRoleName()).collect(Collectors.toList());
        if (Objects.nonNull(list) && list.size() > 0) {
            return Result.error(host.getHostname() + Status.HOST_EXIT_ONE_RUNNING_ROLE.getMsg() + roles.toString());
        }
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(host.getClusterId());
        String clusterCode = clusterInfo.getClusterCode();
        String distributeAgentKey = clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
        if (CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname())) {
            CacheUtils.removeKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname());
        }
        this.removeById(hostId);
        return Result.success();
    }

    @Override
    public Result getRack(Integer clusterId) {
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject rack = new JSONObject();
        rack.put("rack", "/default-rack");
        list.add(rack);
        return Result.success(list);
    }

    @Override
    public void deleteHostByClusterId(Integer clusterId) {
        this.remove(new QueryWrapper<ClusterHostEntity>().eq(Constants.CLUSTER_ID, clusterId));
    }

    @Override
    public void updateBatchNodeLabel(List<String> hostIds, String nodeLabel) {
        List<ClusterHostEntity> list = this.list(new QueryWrapper<ClusterHostEntity>().in(Constants.ID, hostIds));
        for (ClusterHostEntity clusterHostEntity : list) {
            clusterHostEntity.setNodeLabel(nodeLabel);
        }
        this.updateBatchById(list);
    }

    @Override
    public List<ClusterHostEntity> getHostListByIds(List<String> ids) {
        return this.list(new QueryWrapper<ClusterHostEntity>().in(Constants.ID, ids));
    }

    @Override
    public Result assignRack(Integer clusterId, String rack, String hostIds) {
        List<String> ids = Arrays.asList(hostIds.split(","));
        List<ClusterHostEntity> list = this.list(new QueryWrapper<ClusterHostEntity>().in(Constants.ID, ids));
        for (ClusterHostEntity clusterHostEntity : list) {
            clusterHostEntity.setRack(rack);
        }
        this.updateBatchById(list);
        //tell rack actor
        GenerateRackPropCommand command = new GenerateRackPropCommand();
        command.setClusterId(clusterId);
        ActorRef rackActor = ActorUtils.getLocalActor(RackActor.class, "rackActor");
        rackActor.tell(command, ActorRef.noSender());
        return Result.success();
    }

    @Override
    public List<ClusterHostEntity> getClusterHostByRack(Integer clusterId, String rack) {
        return this.list(new QueryWrapper<ClusterHostEntity>()
                .eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.RACK, rack));
    }
}
