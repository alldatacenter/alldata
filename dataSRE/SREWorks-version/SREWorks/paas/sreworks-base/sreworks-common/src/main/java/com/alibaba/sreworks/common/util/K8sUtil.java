package com.alibaba.sreworks.common.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Namespaces;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jinghua.yjh
 */
@Slf4j
public class K8sUtil {

    public static ApiClient client;

    static {
        try {
            if (new File(Config.SERVICEACCOUNT_TOKEN_PATH).exists()) {
                client = Config.fromCluster();
            } else {
                client = Config.defaultClient();
            }
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public static List<V1Service> listServiceForAllNamespaces(String labelSelector) throws ApiException {
        CoreV1Api api = new CoreV1Api();
        V1ServiceList serviceList = api.listServiceForAllNamespaces(null, null, null,
            labelSelector, null, null, null, null, null, null);
        return serviceList.getItems();
    }

    public static List<V1Service> listService(String labelSelector) throws ApiException, IOException {
        String podNamespace = Namespaces.getPodNamespace();
        CoreV1Api api = new CoreV1Api();
        V1ServiceList serviceList = api.listNamespacedService(podNamespace, null, null, null, null,
            labelSelector, null, null, null, null, null);
        return serviceList.getItems();
    }

    public static List<V1Service> listServiceByFieldSelector(String fieldSelector) throws ApiException, IOException {
        String podNamespace = Namespaces.getPodNamespace();
        CoreV1Api api = new CoreV1Api();
        V1ServiceList serviceList = api.listNamespacedService(podNamespace, null, null, null, fieldSelector,
            null, null, null, null, null, null);
        return serviceList.getItems();
    }

    @SuppressWarnings("all")
    public static List<String> getServiceEndpoint(V1Service service) throws IOException, ApiException {
        String name = service.getMetadata().getName();
        String namespace = service.getMetadata().getNamespace();
        return service.getSpec().getPorts().stream()
            .map(port -> String.format("http://%s.%s:%s", name, namespace, port.getPort()))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("all")
    public static String getServiceLabel(V1Service service, String key) throws IOException, ApiException {
        return service.getMetadata().getLabels().get(key);
    }

    public static File getKubeConfigFile(String kubeconfig) throws IOException {
        File file = File.createTempFile("kubeconfig", "");
        file.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(kubeconfig);
        out.close();
        return file;
    }

    public static ApiClient client(String kubeconfig) throws IOException {
        return Config.fromConfig(getKubeConfigFile(kubeconfig).getAbsolutePath());
    }

}
