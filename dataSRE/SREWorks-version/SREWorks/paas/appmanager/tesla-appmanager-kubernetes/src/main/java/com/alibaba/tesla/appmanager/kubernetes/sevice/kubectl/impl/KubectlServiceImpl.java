package com.alibaba.tesla.appmanager.kubernetes.sevice.kubectl.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.domain.req.kubectl.*;
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory;
import com.alibaba.tesla.appmanager.kubernetes.sevice.kubectl.KubectlService;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@Slf4j
public class KubectlServiceImpl implements KubectlService {

    public static final String DEFAULT_NAMESPACE = "apsara-bigdata-manager";

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory;

    /**
     * 获取当前的所有 namespaces
     */
    @Override
    public JSONObject listNamespace(KubectlListNamespaceReq request, String empId) {
        String clusterId = request.getClusterId();
        if (StringUtils.isEmpty(clusterId)) {
            clusterId = "master";
        }
        DefaultKubernetesClient client = kubernetesClientFactory.get(clusterId);
        NamespaceList namespaces = client.namespaces().list();
        return JSONObject.parseObject(JSONObject.toJSONString(namespaces));
    }

    /**
     * 应用 Yaml
     */
    @Override
    public void apply(KubectlApplyReq request, String empId) {
        if (isKindOperatorDeployment(request.getContent())) {
            log.info("skip apply deployment|deployment=app-manager-kind-operator-controller-manager");
            return;
        }

        DefaultKubernetesClient client = kubernetesClientFactory.get(request.getClusterId());
        String namespace = request.getNamespaceId();
        assert DEFAULT_NAMESPACE.equals(namespace);
        String content = JsonUtil.toJsonString(request.getContent());
        try {
            List<HasMetadata> result = client.load(new ByteArrayInputStream(content.getBytes())).get();
            client.resourceList(result).inNamespace(namespace).createOrReplace();
            log.info("apply content to kubernetes success|clusterId={}|namespaceId={}|content={}",
                request.getClusterId(), namespace, content);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("kubectl apply failed, exception=%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    @Override
    public void deleteDeployment(KubectlDeleteDeploymentReq request, String empId) {
        if (isKindOperatorDeploymentName(request.getDeploymentName())) {
            log.info("skip delete deployment|deployment=app-manager-kind-operator-controller-manager");
            return;
        }

        DefaultKubernetesClient client = kubernetesClientFactory.get(request.getClusterId());
        String namespace = request.getNamespaceId();
        assert DEFAULT_NAMESPACE.equals(namespace);
        try {
            client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(request.getDeploymentName())
                .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                .delete();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("kubectl delete deployment failed, exception=%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    @Override
    public void deleteStatefulSet(KubectlDeleteStatefulSetReq request, String empId) {
        DefaultKubernetesClient client = kubernetesClientFactory.get(request.getClusterId());
        String namespace = request.getNamespaceId();
        assert DEFAULT_NAMESPACE.equals(namespace);
        try {
            client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withName(request.getStatefulSetName())
                .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                .delete();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("kubectl delete statefulset failed, exception=%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    @Override
    public void deleteJob(KubectlDeleteJobReq request, String empId) {
        DefaultKubernetesClient client = kubernetesClientFactory.get(request.getClusterId());
        String namespace = request.getNamespaceId();
        assert DEFAULT_NAMESPACE.equals(namespace);
        try {
            JobList jobList = client.batch().jobs().inNamespace(namespace).list();
            for (Job job : jobList.getItems()) {
                if (request.isAsPrefix() && job.getMetadata().getName().startsWith(request.getJobName())) {
                    client.batch()
                        .jobs()
                        .inNamespace(namespace)
                        .withName(job.getMetadata().getName())
                        .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                        .delete();
                } else if (job.getMetadata().getName().equals(request.getJobName())) {
                    client.batch()
                        .jobs()
                        .inNamespace(namespace)
                        .withName(request.getJobName())
                        .withPropagationPolicy(DeletionPropagation.BACKGROUND)
                        .delete();
                }
            }
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("kubectl delete job failed, exception=%s", ExceptionUtils.getStackTrace(e)));
        }
    }

    private static boolean isKindOperatorDeployment(JSONObject content) {
        if (content == null) {
            return false;
        }
        if (!"Deployment".equals(content.getString("kind"))) {
            return false;
        }
        content = JSONObject.parseObject(JSONObject.toJSONString(content));
        try {
            return content.getJSONObject("metadata")
                .getString("name")
                .endsWith("app-manager-kind-operator-controller-manager");
        } catch (Exception e) {
            log.warn("cannot find name in deployment, skip|exception={}", ExceptionUtils.getStackTrace(e));
            return false;
        }
    }

    private static boolean isKindOperatorDeploymentName(String deploymentName) {
        return deploymentName.endsWith("app-manager-kind-operator-controller-manager");
    }
}
