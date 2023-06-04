package com.datasophon.api.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;

import java.util.List;

/**
 * 集群信息表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface ClusterInfoService extends IService<ClusterInfoEntity> {

    ClusterInfoEntity getClusterByClusterCode(String clusterCode);

    Result saveCluster(ClusterInfoEntity clusterInf) ;

    Result getClusterList();

    Result runningClusterList();

    Result updateClusterState(Integer clusterId, Integer clusterState);

    List<ClusterInfoEntity> getClusterByFrameCode(String frameCode);

    Result updateCluster(ClusterInfoEntity clusterInfo);

    void deleteCluster(List<Integer> asList);
}

