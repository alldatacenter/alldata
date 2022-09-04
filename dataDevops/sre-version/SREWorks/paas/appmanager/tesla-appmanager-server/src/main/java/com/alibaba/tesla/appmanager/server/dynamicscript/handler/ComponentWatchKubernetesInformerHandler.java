package com.alibaba.tesla.appmanager.server.dynamicscript.handler;

import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

/**
 * 针对 Kubernetes Informer 类型的组件状态感知 Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentWatchKubernetesInformerHandler extends GroovyHandler {

    /**
     * 集群维度的 informer 事件监听注册器
     *
     * @param clusterId             集群 ID
     * @param namespaceId           限定 Namespace，可为 null 表示全局监听
     * @param client                Kubernetes Client
     * @param sharedInformerFactory 初始化好的 SharedInformerFactory 对象
     */
    void register(String clusterId, String namespaceId, DefaultKubernetesClient client,
                  SharedInformerFactory sharedInformerFactory);

    /**
     * 是否限制到指定的 namespace 中
     *
     * @return namespace 名称
     */
    String restrictNamespace();
}
