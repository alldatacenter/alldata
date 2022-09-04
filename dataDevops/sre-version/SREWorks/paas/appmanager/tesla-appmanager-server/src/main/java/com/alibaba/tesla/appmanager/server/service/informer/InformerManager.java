package com.alibaba.tesla.appmanager.server.service.informer;

import com.alibaba.tesla.appmanager.common.enums.ClusterTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerItem;
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory;
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentWatchKubernetesInformerHandler;
import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import com.alibaba.tesla.appmanager.server.service.cluster.ClusterService;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Cluster Kubernetes Informer 管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class InformerManager {

    /**
     * 存储每个集群 ID (as Key) 对应的 informer 对象
     */
    private final ConcurrentMap<String, SharedInformerFactory> informerMap = new ConcurrentHashMap<>();

    /**
     * 存储每个集群 ID (as Key) 对应的集群配置 MD5，用于检测集群配置否存在更新
     */
    private final ConcurrentMap<String, String> clusterMd5Map = new ConcurrentHashMap<>();

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory;

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

    /**
     * 启动时进行当前 DB 中全量集群的第一次注册工作 (同时进行定时刷新)
     */
    @Scheduled(cron = "${appmanager.cron-job.informer-manager-refresh:0 * * * * *}")
    @SchedulerLock(name = "informerManagerFactoryRefresh")
    public void init() throws IOException {
        Pagination<ClusterDO> clusters = clusterService.list(ClusterQueryCondition.builder()
                .clusterType(ClusterTypeEnum.KUBERNETES.toString())
                .build());
        for (ClusterDO cluster : clusters.getItems()) {
            initCluster(cluster);
        }

        // 通过差值确认需要删除的集群
        HashSet<String> clusterIdSet = new HashSet<>(clusterMd5Map.keySet());
        clusterIdSet.removeAll(clusters.getItems().stream().map(ClusterDO::getClusterId).collect(Collectors.toSet()));
        for (String clusterId : clusterIdSet) {
            removeCluster(clusterId);
        }
    }

    /**
     * 注册指定集群的所有 informer 监听
     *
     * @param cluster 集群对象
     */
    private synchronized void initCluster(ClusterDO cluster) {
        // 前置判断，如果已经存在集群且配置不相等，那么先删除该集群的注册信息
        String clusterId = cluster.getClusterId();
        String clusterMd5 = DigestUtils.md5Hex(cluster.getClusterConfig());
        String currentClusterMd5 = clusterMd5Map.get(clusterId);
        if (currentClusterMd5 != null) {
            // 如果 MD5 相同，那么当前集群无需重复初始化
            if (currentClusterMd5.equals(clusterMd5)) {
                return;
            } else {
                removeCluster(clusterId);
            }
        }

        // 写入 informerMap 和 clusterMd5Map
        DefaultKubernetesClient client = kubernetesClientFactory.get(clusterId);
        SharedInformerFactory informer = client.informers();
        informerMap.put(clusterId, informer);
        clusterMd5Map.put(clusterId, clusterMd5);
        log.info("informer config has updated in cluster {}", clusterId);

        // 遍历全量 Groovy Handlers
        List<GroovyHandlerItem> handlers = groovyHandlerFactory.list();
        for (GroovyHandlerItem handler : handlers) {
            String kind = handler.getKind();
            if (!DynamicScriptKindEnum.COMPONENT_WATCH_KUBERNETES_INFORMER.toString().equals(kind)) {
                continue;
            }

            // 确认为 COMPONENT_WATCH_KUBERNETES_INFORMER 类型后开始注册流程
            ComponentWatchKubernetesInformerHandler informerHandler
                    = (ComponentWatchKubernetesInformerHandler) handler.getGroovyHandler();
            String restrictNamespace = informerHandler.restrictNamespace();
            informerHandler.register(clusterId, restrictNamespace, client, informer);
            log.info("ComponentWatchKubernetesInformerHandler has registered|clusterId={}|restrictNamespace={}|" +
                    "kind={}|name={}", clusterId, restrictNamespace, kind, handler.getName());
        }
        informer.addSharedInformerEventListener(e -> log.error("informer exception occurred|exception={}",
                ExceptionUtils.getStackTrace(e)));
        informer.startAllRegisteredInformers();
        log.info("informers have started in cluster {}", clusterId);
    }

    /**
     * 卸载指定集群的 informer 监听机制
     *
     * @param clusterId 集群 ID
     */
    private synchronized void removeCluster(String clusterId) {
        SharedInformerFactory informer = informerMap.get(clusterId);
        if (informer == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("cannot get informer when remove cluster %s", clusterId));
        }
        informer.stopAllRegisteredInformers();
        informerMap.remove(clusterId);
        clusterMd5Map.remove(clusterId);
        log.info("shutdown all informer in cluster {}", clusterId);
    }
}
