package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.K8sClientProvider;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @ClassName: K8sClientProviderImpl
 * @Author: dyj
 * @DATE: 2021-02-02
 * @Description:
 **/
@Slf4j
@Service
public class K8sClientProviderImpl implements K8sClientProvider {

    private static final String DEFAULT_K8S_CLIENT = "default";
    private static final String DEFAULT_K8S_INFORMER_FACTORY = "default";
//    public static final String DEFAULT_K8S_CLUSTER_ID = "master";

    private static final HashMap<String, ApiClient> apiClientMap = new HashMap<>();
    private static final HashMap<String, SharedInformerFactory> informerFactoryMap = new HashMap<>();

//    @Autowired
//    private KubernetesClientFactory kubernetesClientFactory;

//    @PostConstruct
//    public void init() {
////        String kubeConfigString = kubernetesClientFactory.getKubeConfigString(DEFAULT_K8S_CLUSTER_ID);
//        ApiClient client;
//        SharedInformerFactory informerFactory;
//        try {
//            client = ClientBuilder.defaultClient();
////            client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new StringReader(kubeConfigString))).build();
//            OkHttpClient httpClient = client.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
//            client.setHttpClient(httpClient);
//            informerFactory = new SharedInformerFactory(client);
//        } catch (IOException e) {
//            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
//                "Can not initiate K8sClient or k8sInformerFactory!" + e.getCause());
//        }
//        apiClientMap.put(DEFAULT_K8S_CLIENT, client);
//        informerFactoryMap.put(DEFAULT_K8S_INFORMER_FACTORY, informerFactory);
//        log.info("action=K8sClientProviderImpl|| K8sClientProvider init success!");
//    }

    /**
     * 获取 k8s 客户端
     *
     * @return
     */
    @Override
    public ApiClient getDefaultClient() {
        return apiClientMap.get(DEFAULT_K8S_CLIENT);
    }

    /**
     * 获取默认 k8s informer factory 客户端
     *
     * @return
     */
    @Override
    public SharedInformerFactory getDefaultInformerFactory() {
        return informerFactoryMap.get(DEFAULT_K8S_INFORMER_FACTORY);
    }

    /**
     * 获取 k8s 客户端
     *
     * @param configName
     * @return
     */
    @Override
    public ApiClient getClient(String configName) {
        if (apiClientMap.containsKey(configName)) {
            return apiClientMap.get(configName);
        } else {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "Can not find k8s client name:" + configName);
        }
    }

    /**
     * 获取指定名称的 k8s informer factory 客户端
     *
     * @param informerFactoryName
     * @return
     */
    @Override
    public SharedInformerFactory getInformerFactory(String informerFactoryName) {
        if (informerFactoryMap.containsKey(informerFactoryName)) {
            return informerFactoryMap.get(informerFactoryName);
        } else {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "Can not find k8s client informer factory client:" + informerFactoryName);
        }
    }
}
