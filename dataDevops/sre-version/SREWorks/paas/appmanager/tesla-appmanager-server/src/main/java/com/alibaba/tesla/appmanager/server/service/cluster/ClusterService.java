package com.alibaba.tesla.appmanager.server.service.cluster;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;

/**
 * 集群服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ClusterService {

    /**
     * 根据条件查询 Clusters
     *
     * @param condition 请求数据
     * @return 查询结果
     */
    Pagination<ClusterDO> list(ClusterQueryCondition condition);

    /**
     * 获取指定 clusterId 对应的数据
     *
     * @param clusterId Cluster ID
     * @return 单条记录，如果存不在则返回 null
     */
    ClusterDO get(String clusterId);

    /**
     * 创建 Cluster
     *
     * @param cluster  集群对象
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    ClusterDO insert(ClusterDO cluster, String operator);

    /**
     * 更新指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param cluster   集群对象
     * @param operator  操作人
     * @return 更新后的数据内容
     */
    ClusterDO update(String clusterId, ClusterDO cluster, String operator);

    /**
     * 删除指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param operator  操作人
     */
    void delete(String clusterId, String operator);
}
