package com.alibaba.tesla.appmanager.server.service.cluster.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.ClusterRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import com.alibaba.tesla.appmanager.server.service.cluster.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class ClusterServiceImpl implements ClusterService {

    @Autowired
    private ClusterRepository clusterRepository;

    /**
     * 根据条件查询 Clusters
     *
     * @param condition 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<ClusterDO> list(ClusterQueryCondition condition) {
        List<ClusterDO> result = clusterRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 获取指定 clusterId 对应的数据
     *
     * @param clusterId Cluster ID
     * @return 单条记录，如果存不在则返回 null
     */
    @Override
    public ClusterDO get(String clusterId) {
        if (StringUtils.isEmpty(clusterId)) {
            return null;
        }

        ClusterQueryCondition condition = ClusterQueryCondition.builder().clusterId(clusterId).build();
        List<ClusterDO> clusters = clusterRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(clusters)) {
            return null;
        }
        if (clusters.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple clusters found, clusterId=%s", clusterId));
        }
        return clusters.get(0);
    }

    /**
     * 创建 Cluster
     *
     * @param cluster  集群对象
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    @Override
    public ClusterDO insert(ClusterDO cluster, String operator) {
        int inserted = clusterRepository.insert(cluster);
        log.info("cluster has inserted|cluster={}|inserted={}", JSONObject.toJSONString(cluster), inserted);
        return get(cluster.getClusterId());
    }

    /**
     * 更新指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param cluster   集群对象
     * @param operator  操作人
     * @return 更新后的数据内容
     */
    @Override
    public ClusterDO update(String clusterId, ClusterDO cluster, String operator) {
        int updated = clusterRepository.updateByCondition(
                cluster,
                ClusterQueryCondition.builder().clusterId(clusterId).build()
        );
        log.info("cluster has updated|cluster={}|inserted={}", JSONObject.toJSONString(cluster), updated);
        return get(clusterId);
    }

    /**
     * 删除指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param operator  操作人
     */
    @Override
    public void delete(String clusterId, String operator) {
        int deleted = clusterRepository.deleteByCondition(ClusterQueryCondition.builder().clusterId(clusterId).build());
        log.info("cluster has deleted|clusterId={}|deleted={}|operator={}", clusterId, deleted, operator);
    }
}
