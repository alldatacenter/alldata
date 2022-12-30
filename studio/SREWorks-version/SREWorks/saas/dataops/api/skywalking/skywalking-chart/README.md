# Apache Skywalking Helm Chart

[Apache SkyWalking](https://skywalking.apache.org/) is application performance monitor tool for distributed systems, especially designed for microservices, cloud native and container-based (Docker, K8s, Mesos) architectures.

## Introduction

This chart bootstraps a [Apache SkyWalking](https://skywalking.apache.org/) deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

 - Kubernetes 1.9.6+ 
 - PV dynamic provisioning support on the underlying infrastructure (StorageClass)
 - Helm 3

## Installing the Chart

To install the chart with the release name `my-release`:

```shell
$ helm install my-release skywalking -n <namespace>
```

The command deploys Apache SkyWalking on the Kubernetes cluster in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```shell
$ helm uninstall my-release -n <namespace>
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following table lists the configurable parameters of the Skywalking chart and their default values.

| Parameter                                                    | Description                                                                                      | Default                              |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------|--------------------------------------|
| `nameOverride`                                               | Override name                                                                                    | `nil`                                |
| `serviceAccounts.oap`                                        | Name of the OAP service account to use or create                                                 | `nil`                                |
| `oap.name`                                                   | OAP deployment name                                                                              | `oap`                                |
| `oap.dynamicConfigEnabled`                                     | Enable oap dynamic configuration through k8s configmap                                           | `false`                              |
| `oap.image.repository`                                       | OAP container image name                                                                         | `apache/skywalking-oap-server`       |
| `oap.image.tag`                                              | OAP container image tag                                                                          | `6.1.0`                              |
| `oap.image.pullPolicy`                                       | OAP container image pull policy                                                                  | `IfNotPresent`                       |
| `oap.ports.grpc`                                             | OAP grpc port for tracing or metric                                                              | `11800`                              |
| `oap.ports.rest`                                             | OAP http port for Web UI                                                                         | `12800`                              |
| `oap.replicas`                                               | OAP k8s deployment replicas                                                                      | `2`                                  |
| `oap.service.type`                                           | OAP svc type                                                                                     | `ClusterIP`                          |
| `oap.javaOpts`                                               | Parameters to be added to `JAVA_OPTS`environment variable for OAP                                | `-Xms2g -Xmx2g`                      |
| `oap.antiAffinity`                                           | OAP anti-affinity policy                                                                         | `soft`                               |
| `oap.nodeAffinity`                                           | OAP node affinity policy                                                                         | `{}`                                 |
| `oap.nodeSelector`                                           | OAP labels for master pod assignment                                                             | `{}`                                 |
| `oap.tolerations`                                            | OAP tolerations                                                                                  | `[]`                                 |
| `oap.resources`                                              | OAP node resources requests & limits                                                             | `{} - cpu limit must be an integer`  |
| `oap.envoy.als.enabled`                                      | Open envoy als                                                                                   | `false`                              |
| `oap.env`                                                    | OAP environment variables                                                                        | `[]`                                 |
| `ui.name`                                                    | Web UI deployment name                                                                           | `ui`                                 |
| `ui.replicas`                                                | Web UI k8s deployment replicas                                                                   | `1`                                  |
| `ui.image.repository`                                        | Web UI container image name                                                                      | `apache/skywalking-ui`               |
| `ui.image.tag`                                               | Web UI container image tag                                                                       | `6.1.0`                              |
| `ui.image.pullPolicy`                                        | Web UI container image pull policy                                                               | `IfNotPresent`                       |
| `ui.ingress.enabled`                                         | Create Ingress for Web UI                                                                        | `false`                              |
| `ui.ingress.annotations`                                     | Associate annotations to the Ingress                                                             | `{}`                                 |
| `ui.ingress.path`                                            | Associate path with the Ingress                                                                  | `/`                                  |
| `ui.ingress.hosts`                                           | Associate hosts with the Ingress                                                                 | `[]`                                 |
| `ui.ingress.tls`                                             | Associate TLS with the Ingress                                                                   | `[]`                                 |
| `ui.service.type`                                            | Web UI svc type                                                                                  | `ClusterIP`                          |
| `ui.service.externalPort`                                    | external port for the service                                                                    | `80`                                 |
| `ui.service.internalPort`                                    | internal port for the service                                                                    | `8080`                               |
| `ui.service.externalIPs`                                     | external IP addresses                                                                            | `nil`                                |
| `ui.service.loadBalancerIP`                                  | Load Balancer IP address                                                                         | `nil`                                |
| `ui.service.annotations`                                     | Kubernetes service annotations                                                                   | `{}`                                 |
| `ui.service.loadBalancerSourceRanges`                        | Limit load balancer source IPs to list of CIDRs (where available))                               | `[]`                                 |
| `elasticsearch.enabled`                                      | Spin up a new elasticsearch cluster for SkyWalking                                               | `true`                               |
| `elasticsearch.clusterName`                 | This will be used as the Elasticsearch [cluster.name](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster.name.html) and should be unique per cluster in the namespace                                                                                                                                 | `elasticsearch`                                                                                                           |
| `elasticsearch.nodeGroup`                   | This is the name that will be used for each group of nodes in the cluster. The name will be `clusterName-nodeGroup-X`                                                                                                                                                                                                      | `master`                                                                                                                  |
| `elasticsearch.masterService`               | Optional. The service name used to connect to the masters. You only need to set this if your master `nodeGroup` is set to something other than `master`. See [Clustering and Node Discovery](#clustering-and-node-discovery) for more information.                                                                         | ``                                                                                                                        |
| `elasticsearch.roles`                       | A hash map with the [specific roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html) for the node group                                                                                                                                                                                 | `master: true`<br>`data: true`<br>`ingest: true`                                                                          |
| `elasticsearch.replicas`                    | Kubernetes replica count for the statefulset (i.e. how many pods)                                                                                                                                                                                                                                                          | `3`                                                                                                                       |
| `elasticsearch.minimumMasterNodes`          | The value for [discovery.zen.minimum_master_nodes](https://www.elastic.co/guide/en/elasticsearch/reference/6.7/discovery-settings.html#minimum_master_nodes). Should be set to `(master_eligible_nodes / 2) + 1`. Ignored in Elasticsearch versions >= 7.                                                                  | `2`                                                                                                                       |
| `elasticsearch.esMajorVersion`              | Used to set major version specific configuration. If you are using a custom image and not running the default Elasticsearch version you will need to set this to the version you are running (e.g. `esMajorVersion: 6`)                                                                                                    | `""`                                                                                                                      |
| `elasticsearch.esConfig`                    | Allows you to add any config files in `/usr/share/elasticsearch/config/` such as `elasticsearch.yml` and `log4j2.properties`. See [values.yaml](./values.yaml) for an example of the formatting.                                                                                                                           | `{}`                                                                                                                      |
| `elasticsearch.extraEnvs`                   | Extra [environment variables](https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/#using-environment-variables-inside-of-your-config) which will be appended to the `env:` definition for the container                                                                         | `[]`                                                                                                                      |
| `elasticsearch.extraVolumes`                | Templatable string of additional volumes to be passed to the `tpl` function                                                                                                                                                                                                                                                | `""`                                                                                                                      |
| `elasticsearch.extraVolumeMounts`           | Templatable string of additional volumeMounts to be passed to the `tpl` function                                                                                                                                                                                                                                           | `""`                                                                                                                      |
| `elasticsearch.extraInitContainers`         | Templatable string of additional init containers to be passed to the `tpl` function                                                                                                                                                                                                                                        | `""`                                                                                                                      |
| `elasticsearch.secretMounts`                | Allows you easily mount a secret as a file inside the statefulset. Useful for mounting certificates and other secrets. See [values.yaml](./values.yaml) for an example                                                                                                                                                     | `[]`                                                                                                                      |
| `elasticsearch.image`                       | The Elasticsearch docker image                                                                                                                                                                                                                                                                                             | `docker.elastic.co/elasticsearch/elasticsearch`                                                                           |
| `elasticsearch.imageTag`                    | The Elasticsearch docker image tag                                                                                                                                                                                                                                                                                         | `7.5.1`                                                                                                                   |
| `elasticsearch.imagePullPolicy`             | The Kubernetes [imagePullPolicy](https://kubernetes.io/docs/concepts/containers/images/#updating-images) value                                                                                                                                                                                                             | `IfNotPresent`                                                                                                            |
| `elasticsearch.podAnnotations`              | Configurable [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) applied to all Elasticsearch pods                                                                                                                                                                               | `{}`                                                                                                                      |
| `elasticsearch.labels`                      | Configurable [label](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) applied to all Elasticsearch pods                                                                                                                                                                                          | `{}`                                                                                                                      |
| `elasticsearch.esJavaOpts`                  | [Java options](https://www.elastic.co/guide/en/elasticsearch/reference/current/jvm-options.html) for Elasticsearch. This is where you should configure the [jvm heap size](https://www.elastic.co/guide/en/elasticsearch/reference/current/heap-size.html)                                                                 | `-Xmx1g -Xms1g`                                                                                                           |
| `elasticsearch.resources`                   | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the statefulset                                                                                                                                                                               | `requests.cpu: 100m`<br>`requests.memory: 2Gi`<br>`limits.cpu: 1000m`<br>`limits.memory: 2Gi`                             |
| `elasticsearch.initResources`               | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the initContainer in the statefulset                                                                                                                                                          | {}                                                                                                                        |
| `elasticsearch.sidecarResources`            | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the sidecar containers in the statefulset                                                                                                                                                     | {}                                                                                                                        |
| `elasticsearch.networkHost`                 | Value for the [network.host Elasticsearch setting](https://www.elastic.co/guide/en/elasticsearch/reference/current/network.host.html)                                                                                                                                                                                      | `0.0.0.0`                                                                                                                 |
| `elasticsearch.volumeClaimTemplate`         | Configuration for the [volumeClaimTemplate for statefulsets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#stable-storage). You will want to adjust the storage (default `30Gi`) and the `storageClassName` if you are using a different storage class                                            | `accessModes: [ "ReadWriteOnce" ]`<br>`resources.requests.storage: 30Gi`                                                  |
| `elasticsearch.persistence.annotations`     | Additional persistence annotations for the `volumeClaimTemplate`                                                                                                                                                                                                                                                           | `{}`                                                                                                                      |
| `elasticsearch.persistence.enabled`         | Enables a persistent volume for Elasticsearch data. Can be disabled for nodes that only have [roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html) which don't require persistent data.                                                                                               | `true`                                                                                                                    |
| `elasticsearch.priorityClassName`           | The [name of the PriorityClass](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#priorityclass). No default is supplied as the PriorityClass must be created first.                                                                                                                              | `""`                                                                                                                      |
| `elasticsearch.antiAffinityTopologyKey`     | The [anti-affinity topology key](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity). By default this will prevent multiple Elasticsearch nodes from running on the same Kubernetes node                                                                                        | `kubernetes.io/hostname`                                                                                                  |
| `elasticsearch.antiAffinity`                | Setting this to hard enforces the [anti-affinity rules](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity). If it is set to soft it will be done "best effort". Other values will be ignored.                                                                                  | `hard`                                                                                                                    |
| `elasticsearch.nodeAffinity`                | Value for the [node affinity settings](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#node-affinity-beta-feature)                                                                                                                                                                                      | `{}`                                                                                                                      |
| `elasticsearch.podManagementPolicy`         | By default Kubernetes [deploys statefulsets serially](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#pod-management-policies). This deploys them in parallel so that they can discover eachother                                                                                                   | `Parallel`                                                                                                                |
| `elasticsearch.protocol`                    | The protocol that will be used for the readinessProbe. Change this to `https` if you have `xpack.security.http.ssl.enabled` set                                                                                                                                                                                            | `http`                                                                                                                    |
| `elasticsearch.httpPort`                    | The http port that Kubernetes will use for the healthchecks and the service. If you change this you will also need to set [http.port](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-http.html#_settings) in `extraEnvs`                                                                          | `9200`                                                                                                                    |
| `elasticsearch.transportPort`               | The transport port that Kubernetes will use for the service. If you change this you will also need to set [transport port configuration](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-transport.html#_transport_settings) in `extraEnvs`                                                        | `9300`                                                                                                                    |
| `elasticsearch.service.labels`              | Labels to be added to non-headless service                                                                                                                                                                                                                                                                                 | `{}`                                                                                                                      |
| `elasticsearch.service.labelsHeadless`      | Labels to be added to headless service                                                                                                                                                                                                                                                                                     | `{}`                                                                                                                      |
| `elasticsearch.service.type`                | Type of elasticsearch service. [Service Types](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types)                                                                                                                                                                         | `ClusterIP`                                                                                                               |
| `elasticsearch.service.nodePort`            | Custom [nodePort](https://kubernetes.io/docs/concepts/services-networking/service/#nodeport) port that can be set if you are using `service.type: nodePort`.                                                                                                                                                               | ``                                                                                                                        |
| `elasticsearch.service.annotations`         | Annotations that Kubernetes will use for the service. This will configure load balancer if `service.type` is `LoadBalancer` [Annotations](https://kubernetes.io/docs/concepts/services-networking/service/#ssl-support-on-aws)                                                                                             | `{}`                                                                                                                      |
| `elasticsearch.service.httpPortName`        | The name of the http port within the service                                                                                                                                                                                                                                                                               | `http`                                                                                                                    |
| `elasticsearch.service.transportPortName`   | The name of the transport port within the service                                                                                                                                                                                                                                                                          | `transport`                                                                                                               |
| `elasticsearch.updateStrategy`              | The [updateStrategy](https://kubernetes.io/docs/tutorials/stateful-application/basic-stateful-set/#updating-statefulsets) for the statefulset. By default Kubernetes will wait for the cluster to be green after upgrading each pod. Setting this to `OnDelete` will allow you to manually delete each pod during upgrades | `RollingUpdate`                                                                                                           |
| `elasticsearch.maxUnavailable`              | The [maxUnavailable](https://kubernetes.io/docs/tasks/run-application/configure-pdb/#specifying-a-poddisruptionbudget) value for the pod disruption budget. By default this will prevent Kubernetes from having more than 1 unhealthy pod in the node group                                                                | `1`                                                                                                                       |
| `elasticsearch.fsGroup (DEPRECATED)`        | The Group ID (GID) for [securityContext.fsGroup](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/) so that the Elasticsearch user can read from the persistent volume                                                                                                                            | ``                                                                                                                        |
| `elasticsearch.podSecurityContext`          | Allows you to set the [securityContext](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) for the pod                                                                                                                                                         | `fsGroup: 1000`<br>`runAsUser: 1000`                                                                                      |
| `elasticsearch.securityContext`             | Allows you to set the [securityContext](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container) for the container                                                                                                                                             | `capabilities.drop:[ALL]`<br>`runAsNonRoot: true`<br>`runAsUser: 1000`                                                    |
| `elasticsearch.terminationGracePeriod`      | The [terminationGracePeriod](https://kubernetes.io/docs/concepts/workloads/pods/pod/#termination-of-pods) in seconds used when trying to stop the pod                                                                                                                                                                      | `120`                                                                                                                     |
| `elasticsearch.sysctlInitContainer.enabled` | Allows you to disable the sysctlInitContainer if you are setting vm.max_map_count with another method                                                                                                                                                                                                                      | `true`                                                                                                                    |
| `elasticsearch.sysctlVmMaxMapCount`         | Sets the [sysctl vm.max_map_count](https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html#vm-max-map-count) needed for Elasticsearch                                                                                                                                                        | `262144`                                                                                                                  |
| `elasticsearch.readinessProbe`              | Configuration fields for the [readinessProbe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/)                                                                                                                                                                               | `failureThreshold: 3`<br>`initialDelaySeconds: 10`<br>`periodSeconds: 10`<br>`successThreshold: 3`<br>`timeoutSeconds: 5` |
| `elasticsearch.clusterHealthCheckParams`    | The [Elasticsearch cluster health status params](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html#request-params) that will be used by readinessProbe command                                                                                                                           | `wait_for_status=green&timeout=1s`                                                                                        |
| `elasticsearch.imagePullSecrets`            | Configuration for [imagePullSecrets](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-pod-that-uses-your-secret) so that you can use a private registry for your image                                                                                                       | `[]`                                                                                                                      |
| `elasticsearch.nodeSelector`                | Configurable [nodeSelector](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#nodeselector) so that you can target specific nodes for your Elasticsearch cluster                                                                                                                                          | `{}`                                                                                                                      |
| `elasticsearch.tolerations`                 | Configurable [tolerations](https://kubernetes.io/docs/concepts/configuration/taint-and-toleration/)                                                                                                                                                                                                                        | `[]`                                                                                                                      |
| `elasticsearch.ingress`                     | Configurable [ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) to expose the Elasticsearch service. See [`values.yaml`](./values.yaml) for an example                                                                                                                                            | `enabled: false`                                                                                                          |
| `elasticsearch.schedulerName`               | Name of the [alternate scheduler](https://kubernetes.io/docs/tasks/administer-cluster/configure-multiple-schedulers/#specify-schedulers-for-pods)                                                                                                                                                                          | `nil`                                                                                                                     |
| `elasticsearch.masterTerminationFix`        | A workaround needed for Elasticsearch < 7.2 to prevent master status being lost during restarts [#63](https://github.com/elastic/helm-charts/issues/63)                                                                                                                                                                    | `false`                                                                                                                   |
| `elasticsearch.lifecycle`                   | Allows you to add lifecycle configuration. See [values.yaml](./values.yaml) for an example of the formatting.                                                                                                                                                                                                              | `{}`                                                                                                                      |
| `elasticsearch.keystore`                    | Allows you map Kubernetes secrets into the keystore. See the [config example](/elasticsearch/examples/config/values.yaml) and [how to use the keystore](#how-to-use-the-keystore)                                                                                                                                          | `[]`                                                                                                                      |
| `elasticsearch.rbac`                    | Configuration for creating a role, role binding and service account as part of this helm chart with `create: true`. Also can be used to reference an external service account with `serviceAccountName: "externalServiceAccountName"`.                                                                                             | `create: false`<br>`serviceAccountName: ""`                       |
| `elasticsearch.podSecurityPolicy`       | Configuration for create a pod security policy with minimal permissions to run this Helm chart with `create: true`. Also can be used to reference an external pod security policy with `name: "externalPodSecurityPolicy"`                                                                                                         | `create: false`<br>`name: ""`                                     |

Specify each parameter using the `--set key=value[,key=value]` argument to `helm install`. For example,

```console
$ helm install myrelease skywalking --set nameOverride=newSkywalking
```

Alternatively, a YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,

```console
$ helm install my-release skywalking -f values.yaml
```

> **Tip**: You can use the default [values.yaml](values.yaml)

### RBAC Configuration
Roles and RoleBindings resources will be created automatically for `OAP` .

> **Tip**: You can refer to the default `oap-role.yaml` file in [templates](templates/) to customize your own.

### Ingress TLS
If your cluster allows automatic create/retrieve of TLS certificates (e.g. [kube-lego](https://github.com/jetstack/kube-lego)), please refer to the documentation for that mechanism.

To manually configure TLS, first create/retrieve a key & certificate pair for the address(skywalking ui) you wish to protect. Then create a TLS secret in the namespace:

```console
kubectl create secret tls skywalking-tls --cert=path/to/tls.cert --key=path/to/tls.key
```

Include the secret's name, along with the desired hostnames, in the skywalking-ui Ingress TLS section of your custom `values.yaml` file:

```yaml
ui:
  ingress:
    ## If true, Skywalking ui server Ingress will be created
    ##
    enabled: true

    ## Skywalking ui server Ingress hostnames
    ## Must be provided if Ingress is enabled
    ##
    hosts:
      - skywalking

    ## Skywalking ui server Ingress TLS configuration
    ## Secrets must be manually created in the namespace
    ##
    tls:
      - secretName: skywalking
        hosts:
          - skywalking
```
### Envoy ALS

Envoy ALS(access log service) provides fully logs about RPC routed, including HTTP and TCP.

If you want to open envoy ALS, you can do this by modifying values.yaml. 

```yaml
oap:
  envoy:
    als:
      enabled: true
```

When envoy als ,will give ServiceAccount clusterrole permission.
More envoy als ,please refer to https://github.com/apache/skywalking/blob/master/docs/en/setup/envoy/als_setting.md#observe-service-mesh-through-als
