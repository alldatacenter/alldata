package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.componentpackage.DeleteKanikoPodEvent;
import com.alibaba.tesla.appmanager.server.event.componentpackage.FailedKanikoPodEvent;
import com.alibaba.tesla.appmanager.server.event.componentpackage.SucceedKanikoPodEvent;
import com.alibaba.tesla.appmanager.server.service.componentpackage.instance.constant.PodStatusPhaseEnum;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: K8sInformerFactoryStopListener
 * @Author: dyj
 * @DATE: 2021-02-05
 * @Description:
 **/
@Slf4j
@Component
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class KanikoInformerFactory implements DisposableBean {

    private static final String DEFAULT_K8S_CLIENT = "default";
    private static final String DEFAULT_K8S_INFORMER_FACTORY = "default";
    private static final String DEFAULT_K8S_API = "default";
    private static final String KANIKO_BUILD_PREFIX = "appmanager-build-";

    private static ConcurrentHashMap<String, ApiClient> apiClientMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SharedInformerFactory> informerFactoryMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, CoreV1Api> apiMap = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private SystemProperties systemProperties;

    @PostConstruct
    private void init() {
        if (systemProperties.isEnableKaniko()) {
            ApiClient client;
            SharedInformerFactory informerFactory;
            CoreV1Api api;
            try {
                client = ClientBuilder.defaultClient();
                OkHttpClient httpClient = client.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
                client.setHttpClient(httpClient);
                informerFactory = new SharedInformerFactory(client);
                api = new CoreV1Api(client);
            } catch (IOException e) {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "Can not initiate K8sClient or k8sInformerFactory!" + e.getCause());
            }
            apiClientMap.put(DEFAULT_K8S_CLIENT, client);
            informerFactoryMap.put(DEFAULT_K8S_INFORMER_FACTORY, informerFactory);
            apiMap.put(DEFAULT_K8S_API, api);
            startListenPod();
            log.info("action=K8sClientProviderImpl|| K8sClientProvider init success!");
        }
    }

    private void startListenPod() {
        ApiClient k8sClient = getDefaultClient();
        CoreV1Api api = new CoreV1Api(k8sClient);
        SharedInformerFactory factory = getDefaultInformerFactory();
        SharedIndexInformer<V1Pod> podInformer = factory.getExistingSharedIndexInformer(V1Pod.class);
        if (podInformer == null) {
            podInformer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> api.listNamespacedPodCall(systemProperties.getK8sNamespace(), null,
                            null, null, null, null, null,
                            params.resourceVersion,
                            params.timeoutSeconds,
                            params.watch,
                            null),
                    V1Pod.class,
                    V1PodList.class
            );
            try {
                factory.startAllRegisteredInformers();
            } catch (Exception e) {
                factory.stopAllRegisteredInformers();
                throw new AppException(
                        AppErrorCode.USER_CONFIG_ERROR, "Can not create informer!||message=" + e.getMessage() + "||BackTrace=",
                        e.getStackTrace());
            }
        }
        podInformer.addEventHandler(
                new ResourceEventHandler<V1Pod>() {
                    @Override
                    public void onAdd(V1Pod obj) {
                        if (obj.getMetadata().getName() != null
                                && obj.getMetadata().getName().startsWith(KANIKO_BUILD_PREFIX)) {
                            log.info("actionName=podInformer||add kaniko pod:{}", obj.getMetadata().getName());
                        }

                    }

                    @Override
                    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
                        if (newObj.getMetadata().getName() != null
                                && newObj.getMetadata().getName().startsWith(KANIKO_BUILD_PREFIX)) {
                            log.info("actionName=podInformer||update kaniko pod:{}, new status:{}", newObj.getMetadata().getName(), newObj.getStatus().getPhase());
                            if (PodStatusPhaseEnum.Failed.name().equalsIgnoreCase(newObj.getStatus().getPhase())) {
                                publisher.publishEvent(new FailedKanikoPodEvent(this, newObj.getMetadata().getName()));
                            } else if (PodStatusPhaseEnum.Succeeded.name().equalsIgnoreCase(newObj.getStatus().getPhase())) {
                                publisher.publishEvent(new SucceedKanikoPodEvent(this, newObj.getMetadata().getName()));
                            }
                        }
                    }

                    @Override
                    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
                        if (obj.getMetadata().getName() != null
                                && obj.getMetadata().getName().startsWith(KANIKO_BUILD_PREFIX)) {
                            publisher.publishEvent(new DeleteKanikoPodEvent(this, obj.getMetadata().getName()));
                            log.info("actionName=podInformer||delete kaniko pod:{}, status: {}", obj.getMetadata().getName(), obj.getStatus().getPhase());
                        }
                    }
                });
        podInformer.run();
    }


    /**
     * 获取 k8s 客户端
     *
     * @return
     */
    public ApiClient getDefaultClient() {
        return apiClientMap.get(DEFAULT_K8S_CLIENT);
    }

    /**
     * 获取默认 api
     *
     * @return
     */
    public CoreV1Api getDefaultApi() {
        return apiMap.get(DEFAULT_K8S_API);
    }

    /**
     * 获取默认 k8s informer factory 客户端
     *
     * @return
     */
    public SharedInformerFactory getDefaultInformerFactory() {
        return informerFactoryMap.get(DEFAULT_K8S_INFORMER_FACTORY);
    }

    /**
     * 获取 k8s 客户端
     *
     * @param configName
     * @return
     */
    public ApiClient getClient(String configName) {
        if (apiClientMap.containsKey(configName)) {
            return apiClientMap.get(configName);
        } else {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "Can not find k8s client name:" + configName);
        }

    }

    /**
     * 获取指定名称的 k8s informer factory 客户端
     *
     * @param informerFactoryName
     * @return
     */
    public SharedInformerFactory getInformerFactory(String informerFactoryName) {
        if (informerFactoryMap.containsKey(informerFactoryName)) {
            return informerFactoryMap.get(informerFactoryName);
        } else {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR, "Can not find k8s client informer factory client:" + informerFactoryName);
        }
    }

    @Override
    public void destroy() {
        getDefaultInformerFactory().stopAllRegisteredInformers(true);
    }
}
