package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.ServiceRoleState;

import java.util.List;

/**
 * 集群服务角色实例表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
public interface ClusterServiceRoleInstanceService extends IService<ClusterServiceRoleInstanceEntity> {

    List<ClusterServiceRoleInstanceEntity> getServiceRoleListByHostnameAndClusterId(String hostname, Integer clusterId);

    List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceIdAndRoleState(Integer id, ServiceRoleState stop);

    ClusterServiceRoleInstanceEntity getOneServiceRole(String serviceRoleName, String hostname, Integer clusterId);

    Result listAll(Integer serviceInstanceId, String hostname, Integer serviceRoleState,String serviceRoleName,Integer roleGroupId, Integer page, Integer pageSize);

    Result getLog(Integer serviceRoleInstanceId) throws Exception;

    List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceId(int id);

    Result deleteServiceRole(List<String> idList);

    List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByClusterIdAndRoleName(Integer clusterId, String roleName);

    List<ClusterServiceRoleInstanceEntity> getRunningServiceRoleInstanceListByServiceId(Integer serviceInstanceId);

    Result restartObsoleteService(Integer roleGroupId);

    Result decommissionNode(String serviceRoleInstanceIds,String serviceName) throws Exception;

    void updateToNeedRestart(Integer roleGroupId);

    List<ClusterServiceRoleInstanceEntity> getObsoleteService(Integer id);

    List<ClusterServiceRoleInstanceEntity> getStoppedRoleInstanceOnHost(Integer clusterId, String hostname, ServiceRoleState state);

    void reomveRoleInstance(Integer serviceInstanceId);

    ClusterServiceRoleInstanceEntity getKAdminRoleIns(Integer clusterId);

    List<ClusterServiceRoleInstanceEntity> listServiceRoleByName(String alertManager);

    ClusterServiceRoleInstanceEntity getServiceRoleInsByHostAndName(String hostName, String serviceRoleName);
}

