package com.alibaba.tesla.appmanager.api.provider;

import java.io.IOException;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;

/**
 * @InterfaceName:K8sClientProvider
 * @Author:dyj
 * @DATE: 2021-01-28
 * @Description: 操作 k8s 的访问客户端
 **/
public interface K8sClientProvider {
    /**
     * 获取默认 k8s 客户端
     *
     * @return
     */
    ApiClient getDefaultClient();

    /**
     * 获取默认 k8s informer factory 客户端
     *
     * @return
     */
    SharedInformerFactory getDefaultInformerFactory();

    /**
     * 获取指定名称的 k8s 客户端
     *
     * @return
     */
    ApiClient getClient(String configName);

    /**
     * 获取指定名称的 k8s informer factory 客户端
     *
     * @param informerFactoryName
     * @return
     */
    SharedInformerFactory getInformerFactory(String informerFactoryName);
}
