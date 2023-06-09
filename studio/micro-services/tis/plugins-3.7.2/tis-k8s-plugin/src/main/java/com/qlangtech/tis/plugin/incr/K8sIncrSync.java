/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.plugin.incr;

import com.qlangtech.tis.config.k8s.ReplicasSpec;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.k8s.EnvVarsBuilder;
import com.qlangtech.tis.plugin.k8s.K8SController;
import com.qlangtech.tis.plugin.k8s.K8sExceptionUtils;
import com.qlangtech.tis.plugin.k8s.K8sImage;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

import java.util.List;

/**
 * 弹性扩容：https://github.com/kubernetes-client/java/blob/f20788272291c0e79a8c831d8d5a7dd94d96d2de/kubernetes/docs/AutoscalingV2beta1Api.md
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-12 11:12
 * @date 2020/04/13
 */
public class K8sIncrSync extends K8SController //implements IRCController
{

    // private static final Logger logger = LoggerFactory.getLogger(K8sIncrSync.class);

    // private static final String resultPrettyShow = "true";

    // private final DefaultIncrK8sConfig config;

//    private final K8sImage config;
//
//    private final ApiClient client;
//
//    private final CoreV1Api api;

    public K8sIncrSync(K8sImage k8sConfig) {

        super(k8sConfig, k8sConfig.createApiClient());

//        this.config = k8sConfig;
//        //  client = config.getK8SContext().createConfigInstance();
//        client = ;
//        this.api = new CoreV1Api(client);
    }

    @Override
    public void checkUseable() {
        super.checkUseable();
    }
//    @Override
//    public void relaunch(String collection) {
//
//
//
//        //String namespace, String pretty, Boolean allowWatchBookmarks, String _continue, String fieldSelector, String labelSelector, Integer limit, String resourceVersion, Integer timeoutSeconds, Boolean watch
//        try {
//            // V1PodList v1PodList = api.listNamespacedPod(this.config.namespace, null, null, null, null, "app=" + collection, 100, null, 600, false);
//            V1DeleteOptions options = new V1DeleteOptions();
//            Call call = null;
//            for (V1Pod pod : getRCPods(this.api, this.config, collection)) {
//                //String name, String namespace, String pretty, String dryRun, Integer gracePeriodSeconds, Boolean orphanDependents, String propagationPolicy, V1DeleteOptions body
//                call = api.deleteNamespacedPodCall(pod.getMetadata().getName(), this.config.getNamespace()
//                        , "true", null, 20, true, null, options, null);
//                this.client.execute(call, null);
//                logger.info(" delete pod {}", pod.getMetadata().getName());
//            }
//        } catch (ApiException e) {
//            throw new RuntimeException(collection, e);
//        }
//
//    }

    private static List<V1Pod> getRCPods(CoreV1Api api, K8sImage config, String collection) throws ApiException {

        V1PodList v1PodList = api.listNamespacedPod(config.getNamespace(), null, null
                , null, null, "app=" + collection, 100, null, 600, false);
        return v1PodList.getItems();
    }

    @Override
    public void removeInstance(TargetResName indexName) {
        super.removeInstance(indexName);
//        if (StringUtils.isBlank(indexName)) {
//            throw new IllegalArgumentException("param indexName can not be null");
//        }
//        if (this.config == null || StringUtils.isBlank(this.config.getNamespace())) {
//            throw new IllegalArgumentException("this.config.namespace can not be null");
//        }
//        //String name, String namespace, String pretty, V1DeleteOptions body, String dryRun, Integer gracePeriodSeconds, Boolean orphanDependents, String propagationPolicy
//        //https://raw.githubusercontent.com/kubernetes-client/java/master/kubernetes/docs/CoreV1Api.md
//        V1DeleteOptions body = new V1DeleteOptions();
//        body.setOrphanDependents(true);
//        // Boolean orphanDependents = true;
//        try {
//            // this.api.deleteNamespacedReplicationControllerCall()
//            Call call = this.api.deleteNamespacedReplicationControllerCall(
//                    indexName, this.config.getNamespace(), resultPrettyShow, null, null, true, null, null, null);
//            client.execute(call, null);
//
//            this.relaunch(indexName);
//
//        } catch (ApiException e) {
////            if (ExceptionUtils.indexOfThrowable(e, JsonSyntaxException.class) > -1) {
////                //TODO: 不知道为啥api的代码里面一直没有解决这个问题
////                //https://github.com/kubernetes-client/java/issues/86
////                logger.warn(indexName + e.getMessage());
////                return;
////            } else {
//            throw new RuntimeException(indexName + "\n" + e.getResponseBody(), e);
//            //}
//        }


        // this.api.deleteNamespacedReplicationControllerCall()
    }

    @Override
    public void deploy(TargetResName indexName, ReplicasSpec incrSpec, final long timestamp) throws Exception {
        if (timestamp < 1) {
            throw new IllegalArgumentException("argument timestamp can not small than 1");
        }
        try {
            createReplicationController(indexName, incrSpec, this.addEnvVars(indexName, timestamp));
        } catch (ApiException e) {
            throw K8sExceptionUtils.convert(e);
        }
    }

//    /**
//     * 在k8s容器容器中创建一个RC
//     *
//     * @param config
//     * @param name
//     * @param replicasSpec
//     * @param envs
//     * @throws ApiException
//     */
//    public static void createReplicationController(K8sImage config, CoreV1Api api, String name, ReplicasSpec replicasSpec, List<V1EnvVar> envs) throws ApiException {
//        V1ReplicationController rc = new V1ReplicationController();
//        V1ReplicationControllerSpec spec = new V1ReplicationControllerSpec();
//        spec.setReplicas(replicasSpec.getReplicaCount());
//        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
//        V1ObjectMeta meta = new V1ObjectMeta();
//        meta.setName(name);
//        Map<String, String> labes = Maps.newHashMap();
//        labes.put("app", name);
//        meta.setLabels(labes);
//        templateSpec.setMetadata(meta);
//        V1PodSpec podSpec = new V1PodSpec();
//        List<V1Container> containers = Lists.newArrayList();
//        V1Container c = new V1Container();
//        c.setName(name);
//
//        c.setImage(config.getImagePath());
//        List<V1ContainerPort> ports = Lists.newArrayList();
//        V1ContainerPort port = new V1ContainerPort();
//        port.setContainerPort(8080);
//        port.setName("http");
//        port.setProtocol("TCP");
//        ports.add(port);
//        c.setPorts(ports);
//
//        //V1Container c  c.setEnv(envVars);
//        c.setEnv(envs);
//
//        V1ResourceRequirements rRequirements = new V1ResourceRequirements();
//        Map<String, Quantity> limitQuantityMap = Maps.newHashMap();
//        limitQuantityMap.put("cpu", new Quantity(replicasSpec.getCpuLimit().literalVal()));
//        limitQuantityMap.put("memory", new Quantity(replicasSpec.getMemoryLimit().literalVal()));
//        rRequirements.setLimits(limitQuantityMap);
//        Map<String, Quantity> requestQuantityMap = Maps.newHashMap();
//        requestQuantityMap.put("cpu", new Quantity(replicasSpec.getCpuRequest().literalVal()));
//        requestQuantityMap.put("memory", new Quantity(replicasSpec.getMemoryRequest().literalVal()));
//        rRequirements.setRequests(requestQuantityMap);
//        c.setResources(rRequirements);
//        containers.add(c);
//        if (containers.size() < 1) {
//            throw new IllegalStateException("containers size can not small than 1");
//        }
//        podSpec.setContainers(containers);
//        templateSpec.setSpec(podSpec);
//        spec.setTemplate(templateSpec);
//        rc.setSpec(spec);
//        rc.setApiVersion("v1");
//        meta = new V1ObjectMeta();
//        meta.setName(name);
//        rc.setMetadata(meta);
//
//        api.createNamespacedReplicationController(config.getNamespace(), rc, resultPrettyShow, null, null);
//    }

    private List<V1EnvVar> addEnvVars(TargetResName indexName, long timestamp) {

        EnvVarsBuilder varsBuilder = new EnvVarsBuilder("tis-incr") {
            @Override
            public String getAppOptions() {
                return indexName.getName() + " " + timestamp;
            }
        };
        return varsBuilder.build();
//
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

    }

//    /**
//     * 取得RC实体对象，有即证明已经完成发布流程
//     *
//     * @return
//     */
//    @Override
//    public RcDeployment getRCDeployment(String collection) {
//        return getK8SDeploymentMeta(collection);
//    }

//    public static RcDeployment getRcDeployment(CoreV1Api api, K8sImage config, String tisInstanceName) {
//        Objects.requireNonNull(api, "param api can not be null");
//        Objects.requireNonNull(config, "param config can not be null");
//        RcDeployment rcDeployment = null;
//        try {
//            V1ReplicationController rc = api.readNamespacedReplicationController(tisInstanceName, config.getNamespace(), resultPrettyShow, null, null);
//            if (rc == null) {
//                return null;
//            }
//            rcDeployment = new RcDeployment();
//            rcDeployment.setReplicaCount(rc.getSpec().getReplicas());
//
//            for (V1Container container : rc.getSpec().getTemplate().getSpec().getContainers()) {
//                Objects.requireNonNull(container, "container can not be null");
//                if (container.getEnv() != null) {
//                    for (V1EnvVar env : container.getEnv()) {
//                        rcDeployment.addEnv(env.getName(), env.getValue());
//                    }
//                }
//                rcDeployment.setDockerImage(container.getImage());
//
//                V1ResourceRequirements resources = container.getResources();
//                String cpu = "cpu";
//                String memory = "memory";
//                Map<String, Quantity> requests = resources.getRequests();
//                Map<String, Quantity> limits = resources.getLimits();
//
//                rcDeployment.setMemoryLimit(Specification.parse(limits.get(memory).toSuffixedString()));
//                rcDeployment.setMemoryRequest(Specification.parse(requests.get(memory).toSuffixedString()));
//                rcDeployment.setCpuLimit(Specification.parse(limits.get(cpu).toSuffixedString()));
//                rcDeployment.setCpuRequest(Specification.parse(requests.get(cpu).toSuffixedString()));
//                break;
//            }
//
//            V1ReplicationControllerStatus status = rc.getStatus();
//
//            RcDeployment.ReplicationControllerStatus rControlStatus = new RcDeployment.ReplicationControllerStatus();
//            rControlStatus.setAvailableReplicas(status.getAvailableReplicas());
//            rControlStatus.setFullyLabeledReplicas(status.getFullyLabeledReplicas());
//            rControlStatus.setObservedGeneration(status.getObservedGeneration());
//            rControlStatus.setReadyReplicas(status.getReadyReplicas());
//            rControlStatus.setReplicas(status.getReplicas());
//
//            rcDeployment.setStatus(rControlStatus);
//
//            DateTime creationTimestamp = rc.getMetadata().getCreationTimestamp();
//            rcDeployment.setCreationTimestamp(creationTimestamp.getMillis());
//
//
//            List<V1Pod> rcPods = getRCPods(api, config, tisInstanceName);
//
//            //Call call = api.listNamespacedPodCall(this.config.namespace, null, null, null, null, "app=" + collection, 100, null, 600, true, null);
////            Watch<V1Pod> podWatch = Watch.createWatch(client, call, new TypeToken<Watch.Response<V1Pod>>() {
////            }.getType());
//            V1PodStatus podStatus = null;
//            RcDeployment.PodStatus pods;
//            V1ObjectMeta metadata = null;
//
//            for (V1Pod item : rcPods) {
//                metadata = item.getMetadata();
//                pods = new RcDeployment.PodStatus();
//                pods.setName(metadata.getName());
//                podStatus = item.getStatus();
//                pods.setPhase(podStatus.getPhase());
//                pods.setStartTime(podStatus.getStartTime().getMillis());
//                rcDeployment.addPod(pods);
//            }
//
//        } catch (ApiException e) {
//            if (e.getCode() == 404) {
//                logger.warn("can not get collection rc deployment:" + tisInstanceName);
//                return null;
//            } else {
//                throw new RuntimeException("code:" + e.getCode() + "\n" + e.getResponseBody(), e);
//            }
//        }
//        return rcDeployment;
//    }

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

//    /**
//     * 列表pod，并且显示日志
//     */
//    @Override
//    public WatchPodLog listPodAndWatchLog(String indexName, String podName, ILogListener listener) {
//        DefaultWatchPodLog podlog = new DefaultWatchPodLog(indexName, podName, client, api, config);
//        podlog.addListener(listener);
//        podlog.startProcess();
//        return podlog;
//    }
}
