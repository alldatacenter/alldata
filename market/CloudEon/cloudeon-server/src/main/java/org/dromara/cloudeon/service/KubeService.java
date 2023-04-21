package org.dromara.cloudeon.service;

import org.dromara.cloudeon.dao.ClusterInfoRepository;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class KubeService {

    @Resource
    private ClusterInfoRepository clusterInfoRepository;

    public KubernetesClient getKubeClient(Integer clusterId) {
        String kubeConfig = clusterInfoRepository.findById(clusterId).get().getKubeConfig();
        KubernetesClient client = getKubernetesClient(kubeConfig);
        testConnect(client);
        return client;
    }

    public KubernetesClient getKubernetesClient(String kubeConfig) {
        Config config = Config.fromKubeconfig(kubeConfig);
        KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
        return client;
    }

    public void testConnect(KubernetesClient client) {
        String minor = client.getKubernetesVersion().getMinor();
        log.info("成功连接k8s集群：{}", client.getMasterUrl());
    }
}
