package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.ClusterDTO;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterCreateReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterQueryReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterUpdateReq;

/**
 * Cluster 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ClusterProvider {

    /**
     * 根据条件查询 Clusters
     *
     * @param request 请求数据
     * @return 查询结果
     */
    Pagination<ClusterDTO> queryByCondition(ClusterQueryReq request);

    /**
     * 获取指定 clusterId 对应的数据
     *
     * @param clusterId Cluster ID
     * @return 单条记录，如果存不在则返回 null
     */
    ClusterDTO get(String clusterId);

    /**
     * 创建 Cluster
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    ClusterDTO create(ClusterCreateReq request, String operator);

    /**
     * 更新指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param request     请求更新数据
     * @param operator    操作人
     * @return 更新后的数据内容
     */
    ClusterDTO update(String clusterId, ClusterUpdateReq request, String operator);

    /**
     * 删除指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param operator    操作人
     */
    void delete(String clusterId, String operator);
}
