package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostEntity;

import java.util.List;

/**
 * 集群主机表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
public interface ClusterHostService extends IService<ClusterHostEntity> {

    ClusterHostEntity getClusterHostByHostname(String hostname);

    Result listByPage(Integer clusterId, String hostname,String ip,String cpuArchitecture,Integer hostState,String orderField,String orderType, Integer page, Integer pageSize);

    List<ClusterHostEntity> getHostListByClusterId(Integer id);

    Result getRoleListByHostname(Integer clusterId,String hostname);

    Result deleteHost(Integer hostId);

    Result getRack(Integer clusterId);

    void deleteHostByClusterId(Integer id);

    void updateBatchNodeLabel(List<String> hostIds, String nodeLabel);

    List<ClusterHostEntity> getHostListByIds(List<String> ids);

    Result assignRack(Integer clusterId ,String rack, String hostIds) ;

    List<ClusterHostEntity> getClusterHostByRack(Integer clusterId ,String rack);
}

