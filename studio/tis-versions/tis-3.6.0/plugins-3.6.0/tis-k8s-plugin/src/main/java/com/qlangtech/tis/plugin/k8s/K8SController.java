/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.k8s;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.IRCController;
import com.qlangtech.tis.coredefine.module.action.Specification;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.coredefine.module.action.impl.RcDeployment;
import com.qlangtech.tis.plugin.incr.DefaultWatchPodLog;
import com.qlangtech.tis.plugin.incr.WatchPodLog;
import com.qlangtech.tis.trigger.jst.ILogListener;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import okhttp3.Call;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-06 13:23
 **/
public class K8SController implements IRCController {
    private static final Logger logger = LoggerFactory.getLogger(K8SController.class);

    public static final String resultPrettyShow = "true";


    protected final K8sImage config;

    protected final ApiClient client;
    protected final CoreV1Api api;

    public K8SController(K8sImage k8sConfig, ApiClient client) {
        this.config = k8sConfig;
        this.client = client;
        this.api = new CoreV1Api(client);
    }

    @Override
    public void checkUseable() {

    }

    @Override
    public final void relaunch(TargetResName collection, String... targetPod) {

        //String namespace, String pretty, Boolean allowWatchBookmarks, String _continue, String fieldSelector, String labelSelector, Integer limit, String resourceVersion, Integer timeoutSeconds, Boolean watch
        try {
            // V1PodList v1PodList = api.listNamespacedPod(this.config.namespace, null, null, null, null, "app=" + collection, 100, null, 600, false);
            V1DeleteOptions options = new V1DeleteOptions();
            Call call = null;
            Set<String> targetPods = Sets.newHashSet(targetPod);
            String podName = null;
            for (V1Pod pod : getRCPods(this.api, this.config, collection)) {
                //String name, String namespace, String pretty, String dryRun, Integer gracePeriodSeconds, Boolean orphanDependents, String propagationPolicy, V1DeleteOptions body
                podName = pod.getMetadata().getName();
                if (targetPods.isEmpty() || targetPods.contains(podName)) {
                    call = api.deleteNamespacedPodCall(podName, this.config.getNamespace()
                            , "true", null, 20, true, null, options, null);
                    this.client.execute(call, null);
                    logger.info(" delete pod {}", pod.getMetadata().getName());
                }

            }
        } catch (ApiException e) {
            // throw new RuntimeException(collection, e);
            throw K8sExceptionUtils.convert(collection.getK8SResName(), e);
        }

    }

    private static List<V1Pod> getRCPods(CoreV1Api api, K8sImage config, TargetResName collection) throws ApiException {

        V1PodList v1PodList = api.listNamespacedPod(config.getNamespace(), null, null
                , null, null, "app=" + collection.getK8SResName(), 100, null, 600, false);
        return v1PodList.getItems();
    }


    public void removeInstance(TargetResName indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("param indexName can not be null");
        }
        if (this.config == null || StringUtils.isBlank(this.config.getNamespace())) {
            throw new IllegalArgumentException("this.config.namespace can not be null");
        }
        //String name, String namespace, String pretty, V1DeleteOptions body, String dryRun, Integer gracePeriodSeconds, Boolean orphanDependents, String propagationPolicy
        //https://raw.githubusercontent.com/kubernetes-client/java/master/kubernetes/docs/CoreV1Api.md
        V1DeleteOptions body = new V1DeleteOptions();
        body.setOrphanDependents(true);
        // Boolean orphanDependents = true;
        try {
            // this.api.deleteNamespacedReplicationControllerCall()
            Call call = this.api.deleteNamespacedReplicationControllerCall(
                    indexName.getK8SResName(), this.config.getNamespace(), resultPrettyShow, null, null, true, null, null, null);
            client.execute(call, null);

            this.relaunch(indexName);

        } catch (ApiException e) {
//            if (ExceptionUtils.indexOfThrowable(e, JsonSyntaxException.class) > -1) {
//                //TODO: 不知道为啥api的代码里面一直没有解决这个问题
//                //https://github.com/kubernetes-client/java/issues/86
//                logger.warn(indexName + e.getMessage());
//                return;
//            } else {
            throw K8sExceptionUtils.convert(indexName.getK8SResName(), e); //RuntimeException(indexName + "\n" + e.getResponseBody(), e);
            //}

        }


        // this.api.deleteNamespacedReplicationControllerCall()
    }

    @Override
    public void stopInstance(TargetResName indexName) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void discardSavepoint(TargetResName resName, String savepointPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SupportTriggerSavePointResult supportTriggerSavePoint(TargetResName collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void triggerSavePoint(TargetResName collection) {
        throw new UnsupportedOperationException();
    }

    public static final String REPLICATION_CONTROLLER_VERSION = "v1";

    @Override
    public void deploy(TargetResName collection, ReplicasSpec incrSpec, long timestamp) throws Exception {

    }


    /**
     * 在k8s容器容器中创建一个RC
     *
     * @param
     * @param name
     * @param replicasSpec
     * @param envs
     * @throws ApiException
     */
    public void createReplicationController(TargetResName name, ReplicasSpec replicasSpec, List<V1EnvVar> envs) throws ApiException {
        V1ReplicationController rc = new V1ReplicationController();
        V1ReplicationControllerSpec spec = new V1ReplicationControllerSpec();
        spec.setReplicas(replicasSpec.getReplicaCount());
        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(name.getK8SResName());
        Map<String, String> labes = Maps.newHashMap();
        labes.put("app", name.getK8SResName());
        meta.setLabels(labes);
        templateSpec.setMetadata(meta);
        V1PodSpec podSpec = new V1PodSpec();
        List<V1Container> containers = Lists.newArrayList();
        V1Container c = new V1Container();
        c.setName(name.getK8SResName());

        Objects.requireNonNull(config, "K8sImage can not be null");

        c.setImage(config.getImagePath());
        List<V1ContainerPort> ports = Lists.newArrayList();
        V1ContainerPort port = new V1ContainerPort();
        port.setContainerPort(8080);
        port.setName("http");
        port.setProtocol("TCP");
        ports.add(port);
        c.setPorts(ports);

        //V1Container c  c.setEnv(envVars);
        c.setEnv(envs);

        V1ResourceRequirements rRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limitQuantityMap = Maps.newHashMap();
        limitQuantityMap.put("cpu", new Quantity(replicasSpec.getCpuLimit().literalVal()));
        limitQuantityMap.put("memory", new Quantity(replicasSpec.getMemoryLimit().literalVal()));
        rRequirements.setLimits(limitQuantityMap);
        Map<String, Quantity> requestQuantityMap = Maps.newHashMap();
        requestQuantityMap.put("cpu", new Quantity(replicasSpec.getCpuRequest().literalVal()));
        requestQuantityMap.put("memory", new Quantity(replicasSpec.getMemoryRequest().literalVal()));
        rRequirements.setRequests(requestQuantityMap);
        c.setResources(rRequirements);
        containers.add(c);
        if (containers.size() < 1) {
            throw new IllegalStateException("containers size can not small than 1");
        }

        List<HostAlias> hostAliases = config.getHostAliases();
        if (CollectionUtils.isNotEmpty(hostAliases)) {
            List<V1HostAlias> setHostAliases = Lists.newArrayList();
            V1HostAlias v1host = null;
            for (HostAlias ha : hostAliases) {
                v1host = new V1HostAlias();
                v1host.setIp(ha.getIp());
                v1host.setHostnames(ha.getHostnames());
                setHostAliases.add(v1host);
            }
            podSpec.setHostAliases(setHostAliases);
        }


        podSpec.setContainers(containers);
        templateSpec.setSpec(podSpec);
        spec.setTemplate(templateSpec);
        rc.setSpec(spec);
        rc.setApiVersion(REPLICATION_CONTROLLER_VERSION);
        meta = new V1ObjectMeta();
        meta.setName(name.getK8SResName());
        rc.setMetadata(meta);

        api.createNamespacedReplicationController(config.getNamespace(), rc, resultPrettyShow, null, null);
    }

//    private List<V1EnvVar> addEnvVars(String indexName, long timestamp) {
//        List<V1EnvVar> envVars = Lists.newArrayList();
//        V1EnvVar var = new V1EnvVar();
//        var.setName("JVM_PROPERTY");
//        var.setValue("-Ddata.dir=/opt/data -D" + Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS + "=true");
//        envVars.add(var);
//
//        RunEnvironment runtime = RunEnvironment.getSysRuntime();
//        var = new V1EnvVar();
//        var.setName("JAVA_RUNTIME");
//        var.setValue(runtime.getKeyName());
//        envVars.add(var);
//        var = new V1EnvVar();
//        var.setName("APP_OPTIONS");
//        var.setValue(indexName + " " + timestamp);
//        envVars.add(var);
//
//        var = new V1EnvVar();
//        var.setName("APP_NAME");
//        var.setValue("tis-incr");
//        envVars.add(var);
//
//        var = new V1EnvVar();
//        var.setName(Config.KEY_RUNTIME);
//        var.setValue(runtime.getKeyName());
//        envVars.add(var);
//
//        var = new V1EnvVar();
//        var.setName(Config.KEY_ZK_HOST);
//        var.setValue(Config.getZKHost());
//        envVars.add(var);
//
//        var = new V1EnvVar();
//        var.setName(Config.KEY_ASSEMBLE_HOST);
//        var.setValue(Config.getAssembleHost());
//        envVars.add(var);
//
//        var = new V1EnvVar();
//        var.setName(Config.KEY_TIS_HOST);
//        var.setValue(Config.getTisHost());
//        envVars.add(var);
//
//        return envVars;
//
//    }

//    /**
//     * 取得RC实体对象，有即证明已经完成发布流程
//     *
//     * @return
//     */
//    @Override
//    public RcDeployment getRCDeployment(String collection) {
//        return getRcDeployment(api, this.config, collection);
//    }

    @Override
    public RcDeployment getRCDeployment(TargetResName tisInstanceName) {

        Objects.requireNonNull(api, "param api can not be null");
        Objects.requireNonNull(config, "param config can not be null");
        RcDeployment rcDeployment = null;
        try {

            V1ReplicationController rc = api.readNamespacedReplicationController(
                    tisInstanceName.getK8SResName(), config.getNamespace(), resultPrettyShow, null, null);
            if (rc == null) {
                return null;
            }
            rcDeployment = new RcDeployment();
            fillSpecInfo(rcDeployment, rc.getSpec().getReplicas(), rc.getSpec().getTemplate());

            V1ReplicationControllerStatus status = rc.getStatus();
            RcDeployment.ReplicationControllerStatus rControlStatus = new RcDeployment.ReplicationControllerStatus();
            rControlStatus.setAvailableReplicas(status.getAvailableReplicas());
            rControlStatus.setFullyLabeledReplicas(status.getFullyLabeledReplicas());
            rControlStatus.setObservedGeneration(status.getObservedGeneration());
            rControlStatus.setReadyReplicas(status.getReadyReplicas());
            rControlStatus.setReplicas(status.getReplicas());

            rcDeployment.setStatus(rControlStatus);

            fillCreateTimestamp(rcDeployment, rc.getMetadata());


            fillPods(this.api, this.config, rcDeployment, tisInstanceName);

        } catch (ApiException e) {
            if (e.getCode() == 404) {
                logger.warn("can not get collection rc deployment:" + tisInstanceName.getK8SResName());
                return null;
            } else {
                throw K8sExceptionUtils.convert("code:" + e.getCode(), e); //new RuntimeException("code:" + e.getCode() + "\n" + e.getResponseBody(), e);
            }
        }
        return rcDeployment;
    }

    public static void fillPods(CoreV1Api api, K8sImage config, RcDeployment rcDeployment, TargetResName tisInstanceName) throws ApiException {
        List<V1Pod> rcPods = getRCPods(api, config, tisInstanceName);

        V1PodStatus podStatus = null;
        RcDeployment.PodStatus pods;
        V1ObjectMeta metadata = null;

        for (V1Pod item : rcPods) {
            metadata = item.getMetadata();
            pods = new RcDeployment.PodStatus();
            pods.setName(metadata.getName());
            podStatus = item.getStatus();
            int restartCount = 0;
            for (V1ContainerStatus cstat : podStatus.getContainerStatuses()) {
                restartCount += cstat.getRestartCount();
            }
            pods.setRestartCount(restartCount);
            pods.setPhase(podStatus.getPhase());
            pods.setStartTime(podStatus.getStartTime().getMillis());
            rcDeployment.addPod(pods);
        }
    }

    public static void fillCreateTimestamp(RcDeployment rcDeployment, V1ObjectMeta meta) {
        DateTime creationTimestamp = meta.getCreationTimestamp();
        rcDeployment.setCreationTimestamp(creationTimestamp.getMillis());
    }

    public static void fillSpecInfo(RcDeployment rcDeployment, int replicasCount, V1PodTemplateSpec spec) {
        rcDeployment.setReplicaCount(replicasCount);

        for (V1Container container : spec.getSpec().getContainers()) {
            Objects.requireNonNull(container, "container can not be null");
            if (container.getEnv() != null) {
                for (V1EnvVar env : container.getEnv()) {
                    rcDeployment.addEnv(env.getName(), env.getValue());
                }
            }
            rcDeployment.setDockerImage(container.getImage());

            V1ResourceRequirements resources = container.getResources();
            String cpu = "cpu";
            String memory = "memory";
            Map<String, Quantity> requests = resources.getRequests();
            Map<String, Quantity> limits = resources.getLimits();

            rcDeployment.setMemoryLimit(Specification.parse(limits.get(memory).toSuffixedString()));
            rcDeployment.setMemoryRequest(Specification.parse(requests.get(memory).toSuffixedString()));
            rcDeployment.setCpuLimit(Specification.parse(limits.get(cpu).toSuffixedString()));
            rcDeployment.setCpuRequest(Specification.parse(requests.get(cpu).toSuffixedString()));
            break;
        }
    }

//    /**
//     * 是否存在RC，有即证明已经完成发布流程
//     *
//     * @return
//     */
//    public boolean isRCDeployment(String indexName) {
//        try {
//            V1ReplicationController rc = api.readNamespacedReplicationController(indexName, this.config.namespace, resultPrettyShow, null, null);
//            return rc != null;
//        } catch (ApiException e) {
//            if (e.getCode() == 404) {
//                return false;
//            } else {
//                throw new RuntimeException("code:" + e.getCode(), e);
//            }
//        }
//    }

    /**
     * 列表pod，并且显示日志
     */
    public final WatchPodLog listPodAndWatchLog(TargetResName indexName, String podName, ILogListener listener) {
        return listPodAndWatchLog(client, config, indexName.getK8SResName(), indexName, podName, listener);
//        DefaultWatchPodLog podlog = new DefaultWatchPodLog(indexName, podName, client, api, config);
//        podlog.addListener(listener);
//        podlog.startProcess();
//        return podlog;
    }


    public static WatchPodLog listPodAndWatchLog(ApiClient client, final K8sImage config
            , String containerId, TargetResName indexName, String podName, ILogListener listener) {
        DefaultWatchPodLog podlog = new DefaultWatchPodLog(containerId, indexName, podName, client, config);
        podlog.addListener(listener);
        podlog.startProcess();
        return podlog;
    }
}
