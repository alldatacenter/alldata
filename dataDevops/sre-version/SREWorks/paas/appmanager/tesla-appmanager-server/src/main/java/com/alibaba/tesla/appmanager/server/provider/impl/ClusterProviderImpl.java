package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ClusterProvider;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.domain.dto.ClusterDTO;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterCreateReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterQueryReq;
import com.alibaba.tesla.appmanager.domain.req.cluster.ClusterUpdateReq;
import com.alibaba.tesla.appmanager.server.assembly.ClusterDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import com.alibaba.tesla.appmanager.server.service.cluster.ClusterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * Cluster 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class ClusterProviderImpl implements ClusterProvider {

    @Autowired
    private ClusterService clusterService;

    @Resource
    private ClusterDtoConvert clusterConvert;

    /**
     * 根据条件查询 Clusters
     *
     * @param request 请求数据
     * @return 查询结果
     */
    @Override
    public Pagination<ClusterDTO> queryByCondition(ClusterQueryReq request) {
        ClusterQueryCondition condition = new ClusterQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<ClusterDO> results = clusterService.list(condition);
        return Pagination.transform(results, item -> clusterConvert.to(item));
    }

    /**
     * 获取指定 clusterId 对应的数据
     *
     * @param clusterId Cluster ID
     * @return 单条记录，如果存不在则返回 null
     */
    @Override
    public ClusterDTO get(String clusterId) {
        return clusterConvert.to(clusterService.get(clusterId));
    }

    /**
     * 创建 Cluster
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 创建后的数据内容
     */
    @Override
    public ClusterDTO create(ClusterCreateReq request, String operator) {
        String clusterId = request.getClusterId();
        if (checkExist(clusterId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("the specified clusterId %s already exists", clusterId));
        }
        JSONObject clusterConfig = request.getClusterConfig();
        if (clusterConfig.containsKey("kube")
                || (clusterConfig.containsKey("masterUrl") && clusterConfig.containsKey("oauthToken"))) {
            ClusterDO cluster = ClusterDO.builder()
                    .clusterId(clusterId)
                    .clusterName(request.getClusterName())
                    .clusterType(request.getClusterType())
                    .clusterConfig(JsonUtil.toJsonString(clusterConfig))
                    .masterFlag(request.getMasterFlag())
                    .build();
            cluster = clusterService.insert(cluster, operator);
            log.info("cluster has inserted|cluster={}|operator={}", JSONObject.toJSONString(cluster), operator);
            return clusterConvert.to(cluster);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid cluster configuration");
        }
    }

    /**
     * 更新指定的 Cluster
     *
     * @param clusterId 定位 ClusterId
     * @param request   请求更新数据
     * @param operator  操作人
     * @return 更新后的数据内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClusterDTO update(String clusterId, ClusterUpdateReq request, String operator) {
        ClusterDO clusterDO = ClusterDO.builder()
                .clusterName(request.getClusterName())
                .clusterType(request.getClusterType())
                .clusterConfig(request.getClusterConfig() != null ? JsonUtil.toJsonString(request.getClusterConfig()) : null)
                .masterFlag(request.getMasterFlag())
                .build();
        clusterService.update(clusterId, clusterDO, operator);
        log.info("set cluster {} masterFlag to true", clusterId);

        // 如果设置某个集群的 masterFlag 为 true，那么设置其余所有 cluster 的 masterFlag 为 false
        if (request.getMasterFlag() != null && request.getMasterFlag()) {
            ClusterQueryCondition condition = ClusterQueryCondition.builder().build();
            Pagination<ClusterDO> allClusters = clusterService.list(condition);
            for (ClusterDO cluster : allClusters.getItems()) {
                if (cluster.getClusterId().equals(clusterId)) {
                    continue;
                }
                clusterService.update(cluster.getClusterId(), ClusterDO.builder().masterFlag(false).build(), operator);
                log.info("set cluster {} masterFlag to false", cluster.getClusterId());
            }
        }
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
        clusterService.delete(clusterId, operator);
    }

    /**
     * 检测指定 clusterId 是否存在对应记录
     *
     * @param clusterId 定位 ClusterId
     * @return true or false
     */
    private boolean checkExist(String clusterId) {
        ClusterDTO clusterDTO = get(clusterId);
        return clusterDTO != null;
    }
}
