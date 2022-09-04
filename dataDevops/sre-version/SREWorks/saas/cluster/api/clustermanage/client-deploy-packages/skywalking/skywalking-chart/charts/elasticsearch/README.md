# Elasticsearch Helm Chart

This functionality is in beta and is subject to change. The design and code is less mature than official GA features and is being provided as-is with no warranties. Beta features are not subject to the support SLA of official GA features.

This helm chart is a lightweight way to configure and run our official [Elasticsearch docker image](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html)

## Requirements

* [Helm](https://helm.sh/) >=2.8.0 and <3.0.0 (see parent [README](https://github.com/elastic/helm-charts/tree/master/README.md) for more details)
* Kubernetes >=1.8
* Minimum cluster requirements include the following to run this chart with default settings. All of these settings are configurable.
  * Three Kubernetes nodes to respect the default "hard" affinity settings
  * 1GB of RAM for the JVM heap

## Usage notes and getting started

* This repo includes a number of [example](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples) configurations which can be used as a reference. They are also used in the automated testing of this chart
* Automated testing of this chart is currently only run against GKE (Google Kubernetes Engine).
* The chart deploys a statefulset and by default will do an automated rolling update of your cluster. It does this by waiting for the cluster health to become green after each instance is updated. If you prefer to update manually you can set [`updateStrategy: OnDelete`](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#on-delete)
* It is important to verify that the JVM heap size in `esJavaOpts` and to set the CPU/Memory `resources` to something suitable for your cluster
* To simplify chart and maintenance each set of node groups is deployed as a separate helm release. Take a look at the [multi](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples/multi) example to get an idea for how this works. Without doing this it isn't possible to resize persistent volumes in a statefulset. By setting it up this way it makes it possible to add more nodes with a new storage size then drain the old ones. It also solves the problem of allowing the user to determine which node groups to update first when doing upgrades or changes.
* We have designed this chart to be very un-opinionated about how to configure Elasticsearch. It exposes ways to set environment variables and mount secrets inside of the container. Doing this makes it much easier for this chart to support multiple versions with minimal changes.

## Migration from helm/charts stable

If you currently have a cluster deployed with the [helm/charts stable](https://github.com/helm/charts/tree/master/stable/elasticsearch) chart you can follow the [migration guide](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples/migration/README.md)

## Installing

### Using Helm repository

* Add the elastic helm charts repo
  ```
  helm repo add elastic https://helm.elastic.co
  ```
* Install it
  ```
  helm install --name elasticsearch elastic/elasticsearch
  ```

### Using master branch

* Clone the git repo
  ```
  git clone git@github.com:elastic/helm-charts.git
  ```
* Install it
  ```
  helm install --name elasticsearch ./helm-charts/elasticsearch
  ```

## Compatibility

This chart is tested with the latest supported versions. The currently tested versions are:

| 6.x   | 7.x   |
| ----- | ----- |
| 6.8.6 | 7.5.2 |

Examples of installing older major versions can be found in the [examples](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples) directory.

While only the latest releases are tested, it is possible to easily install old or new releases by overriding the `imageTag`. To install version `7.5.2` of Elasticsearch it would look like this:

```
helm install --name elasticsearch elastic/elasticsearch --set imageTag=7.5.2
```

## Configuration

| Parameter                     | Description                                                                                                                                                                                                                                                                                                                 | Default                                                                                                                   |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `clusterName`                 | This will be used as the Elasticsearch [cluster.name](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster.name.html) and should be unique per cluster in the namespace                                                                                                                                  | `elasticsearch`                                                                                                           |
| `nodeGroup`                   | This is the name that will be used for each group of nodes in the cluster. The name will be `clusterName-nodeGroup-X`, `nameOverride-nodeGroup-X` if a nameOverride is specified, and `fullnameOverride-X` if a fullnameOverride is specified                                                                               | `master`                                                                                                                  |
| `masterService`               | Optional. The service name used to connect to the masters. You only need to set this if your master `nodeGroup` is set to something other than `master`. See [Clustering and Node Discovery](https://github.com/elastic/helm-charts/tree/master/elasticsearch/README.md#clustering-and-node-discovery) for more information | ``                                                                                                                        |
| `roles`                       | A hash map with the [specific roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html) for the node group                                                                                                                                                                                  | `master: true`<br>`data: true`<br>`ingest: true`                                                                          |
| `replicas`                    | Kubernetes replica count for the statefulset (i.e. how many pods)                                                                                                                                                                                                                                                           | `3`                                                                                                                       |
| `minimumMasterNodes`          | The value for [discovery.zen.minimum_master_nodes](https://www.elastic.co/guide/en/elasticsearch/reference/6.7/discovery-settings.html#minimum_master_nodes). Should be set to `(master_eligible_nodes / 2) + 1`. Ignored in Elasticsearch versions >= 7.                                                                   | `2`                                                                                                                       |
| `esMajorVersion`              | Used to set major version specific configuration. If you are using a custom image and not running the default Elasticsearch version you will need to set this to the version you are running (e.g. `esMajorVersion: 6`)                                                                                                     | `""`                                                                                                                      |
| `esConfig`                    | Allows you to add any config files in `/usr/share/elasticsearch/config/` such as `elasticsearch.yml` and `log4j2.properties`. See [values.yaml](https://github.com/elastic/helm-charts/tree/master/elasticsearch/values.yaml) for an example of the formatting.                                                             | `{}`                                                                                                                      |
| `extraEnvs`                   | Extra [environment variables](https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/#using-environment-variables-inside-of-your-config) which will be appended to the `env:` definition for the container                                                                          | `[]`                                                                                                                      |
| `extraVolumes`                | Templatable string of additional volumes to be passed to the `tpl` function                                                                                                                                                                                                                                                 | `""`                                                                                                                      |
| `extraVolumeMounts`           | Templatable string of additional volumeMounts to be passed to the `tpl` function                                                                                                                                                                                                                                            | `""`                                                                                                                      |
| `extraInitContainers`         | Templatable string of additional init containers to be passed to the `tpl` function                                                                                                                                                                                                                                         | `""`                                                                                                                      |
| `secretMounts`                | Allows you easily mount a secret as a file inside the statefulset. Useful for mounting certificates and other secrets. See [values.yaml](https://github.com/elastic/helm-charts/tree/master/elasticsearch/values.yaml) for an example                                                                                       | `[]`                                                                                                                      |
| `image`                       | The Elasticsearch docker image                                                                                                                                                                                                                                                                                              | `docker.elastic.co/elasticsearch/elasticsearch`                                                                           |
| `imageTag`                    | The Elasticsearch docker image tag                                                                                                                                                                                                                                                                                          | `7.5.2`                                                                                                                   |
| `imagePullPolicy`             | The Kubernetes [imagePullPolicy](https://kubernetes.io/docs/concepts/containers/images/#updating-images) value                                                                                                                                                                                                              | `IfNotPresent`                                                                                                            |
| `podAnnotations`              | Configurable [annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) applied to all Elasticsearch pods                                                                                                                                                                                | `{}`                                                                                                                      |
| `labels`                      | Configurable [label](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/) applied to all Elasticsearch pods                                                                                                                                                                                           | `{}`                                                                                                                      |
| `esJavaOpts`                  | [Java options](https://www.elastic.co/guide/en/elasticsearch/reference/current/jvm-options.html) for Elasticsearch. This is where you should configure the [jvm heap size](https://www.elastic.co/guide/en/elasticsearch/reference/current/heap-size.html)                                                                  | `-Xmx1g -Xms1g`                                                                                                           |
| `resources`                   | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the statefulset                                                                                                                                                                                | `requests.cpu: 100m`<br>`requests.memory: 2Gi`<br>`limits.cpu: 1000m`<br>`limits.memory: 2Gi`                             |
| `initResources`               | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the initContainer in the statefulset                                                                                                                                                           | {}                                                                                                                        |
| `sidecarResources`            | Allows you to set the [resources](https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/) for the sidecar containers in the statefulset                                                                                                                                                      | {}                                                                                                                        |
| `networkHost`                 | Value for the [network.host Elasticsearch setting](https://www.elastic.co/guide/en/elasticsearch/reference/current/network.host.html)                                                                                                                                                                                       | `0.0.0.0`                                                                                                                 |
| `volumeClaimTemplate`         | Configuration for the [volumeClaimTemplate for statefulsets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#stable-storage). You will want to adjust the storage (default `30Gi`) and the `storageClassName` if you are using a different storage class                                             | `accessModes: [ "ReadWriteOnce" ]`<br>`resources.requests.storage: 30Gi`                                                  |
| `persistence.annotations`     | Additional persistence annotations for the `volumeClaimTemplate`                                                                                                                                                                                                                                                            | `{}`                                                                                                                      |
| `persistence.enabled`         | Enables a persistent volume for Elasticsearch data. Can be disabled for nodes that only have [roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html) which don't require persistent data.                                                                                                | `true`                                                                                                                    |
| `priorityClassName`           | The [name of the PriorityClass](https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#priorityclass). No default is supplied as the PriorityClass must be created first.                                                                                                                               | `""`                                                                                                                      |
| `antiAffinityTopologyKey`     | The [anti-affinity topology key](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity). By default this will prevent multiple Elasticsearch nodes from running on the same Kubernetes node                                                                                         | `kubernetes.io/hostname`                                                                                                  |
| `antiAffinity`                | Setting this to hard enforces the [anti-affinity rules](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity). If it is set to soft it will be done "best effort". Other values will be ignored.                                                                                   | `hard`                                                                                                                    |
| `nodeAffinity`                | Value for the [node affinity settings](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#node-affinity-beta-feature)                                                                                                                                                                                       | `{}`                                                                                                                      |
| `podManagementPolicy`         | By default Kubernetes [deploys statefulsets serially](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#pod-management-policies). This deploys them in parallel so that they can discover eachother                                                                                                    | `Parallel`                                                                                                                |
| `protocol`                    | The protocol that will be used for the readinessProbe. Change this to `https` if you have `xpack.security.http.ssl.enabled` set                                                                                                                                                                                             | `http`                                                                                                                    |
| `httpPort`                    | The http port that Kubernetes will use for the healthchecks and the service. If you change this you will also need to set [http.port](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-http.html#_settings) in `extraEnvs`                                                                           | `9200`                                                                                                                    |
| `transportPort`               | The transport port that Kubernetes will use for the service. If you change this you will also need to set [transport port configuration](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-transport.html#_transport_settings) in `extraEnvs`                                                         | `9300`                                                                                                                    |
| `service.labels`              | Labels to be added to non-headless service                                                                                                                                                                                                                                                                                  | `{}`                                                                                                                      |
| `service.labelsHeadless`      | Labels to be added to headless service                                                                                                                                                                                                                                                                                      | `{}`                                                                                                                      |
| `service.type`                | Type of elasticsearch service. [Service Types](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types)                                                                                                                                                                          | `ClusterIP`                                                                                                               |
| `service.nodePort`            | Custom [nodePort](https://kubernetes.io/docs/concepts/services-networking/service/#nodeport) port that can be set if you are using `service.type: nodePort`.                                                                                                                                                                | ``                                                                                                                        |
| `service.annotations`         | Annotations that Kubernetes will use for the service. This will configure load balancer if `service.type` is `LoadBalancer` [Annotations](https://kubernetes.io/docs/concepts/services-networking/service/#ssl-support-on-aws)                                                                                              | `{}`                                                                                                                      |
| `service.httpPortName`        | The name of the http port within the service                                                                                                                                                                                                                                                                                | `http`                                                                                                                    |
| `service.transportPortName`   | The name of the transport port within the service                                                                                                                                                                                                                                                                           | `transport`                                                                                                               |
| `updateStrategy`              | The [updateStrategy](https://kubernetes.io/docs/tutorials/stateful-application/basic-stateful-set/#updating-statefulsets) for the statefulset. By default Kubernetes will wait for the cluster to be green after upgrading each pod. Setting this to `OnDelete` will allow you to manually delete each pod during upgrades  | `RollingUpdate`                                                                                                           |
| `maxUnavailable`              | The [maxUnavailable](https://kubernetes.io/docs/tasks/run-application/configure-pdb/#specifying-a-poddisruptionbudget) value for the pod disruption budget. By default this will prevent Kubernetes from having more than 1 unhealthy pod in the node group                                                                 | `1`                                                                                                                       |
| `fsGroup (DEPRECATED)`        | The Group ID (GID) for [securityContext.fsGroup](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/) so that the Elasticsearch user can read from the persistent volume                                                                                                                             | ``                                                                                                                        |
| `podSecurityContext`          | Allows you to set the [securityContext](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) for the pod                                                                                                                                                          | `fsGroup: 1000`<br>`runAsUser: 1000`                                                                                      |
| `securityContext`             | Allows you to set the [securityContext](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container) for the container                                                                                                                                              | `capabilities.drop:[ALL]`<br>`runAsNonRoot: true`<br>`runAsUser: 1000`                                                    |
| `terminationGracePeriod`      | The [terminationGracePeriod](https://kubernetes.io/docs/concepts/workloads/pods/pod/#termination-of-pods) in seconds used when trying to stop the pod                                                                                                                                                                       | `120`                                                                                                                     |
| `sysctlInitContainer.enabled` | Allows you to disable the sysctlInitContainer if you are setting vm.max_map_count with another method                                                                                                                                                                                                                       | `true`                                                                                                                    |
| `sysctlVmMaxMapCount`         | Sets the [sysctl vm.max_map_count](https://www.elastic.co/guide/en/elasticsearch/reference/current/vm-max-map-count.html#vm-max-map-count) needed for Elasticsearch                                                                                                                                                         | `262144`                                                                                                                  |
| `readinessProbe`              | Configuration fields for the [readinessProbe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/)                                                                                                                                                                                | `failureThreshold: 3`<br>`initialDelaySeconds: 10`<br>`periodSeconds: 10`<br>`successThreshold: 3`<br>`timeoutSeconds: 5` |
| `clusterHealthCheckParams`    | The [Elasticsearch cluster health status params](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html#request-params) that will be used by readinessProbe command                                                                                                                            | `wait_for_status=green&timeout=1s`                                                                                        |
| `imagePullSecrets`            | Configuration for [imagePullSecrets](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-pod-that-uses-your-secret) so that you can use a private registry for your image                                                                                                        | `[]`                                                                                                                      |
| `nodeSelector`                | Configurable [nodeSelector](https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#nodeselector) so that you can target specific nodes for your Elasticsearch cluster                                                                                                                                           | `{}`                                                                                                                      |
| `tolerations`                 | Configurable [tolerations](https://kubernetes.io/docs/concepts/configuration/taint-and-toleration/)                                                                                                                                                                                                                         | `[]`                                                                                                                      |
| `ingress`                     | Configurable [ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/) to expose the Elasticsearch service. See [`values.yaml`](https://github.com/elastic/helm-charts/tree/master/elasticsearch/values.yaml) for an example                                                                              | `enabled: false`                                                                                                          |
| `schedulerName`               | Name of the [alternate scheduler](https://kubernetes.io/docs/tasks/administer-cluster/configure-multiple-schedulers/#specify-schedulers-for-pods)                                                                                                                                                                           | `nil`                                                                                                                     |
| `masterTerminationFix`        | A workaround needed for Elasticsearch < 7.2 to prevent master status being lost during restarts [#63](https://github.com/elastic/helm-charts/issues/63)                                                                                                                                                                     | `false`                                                                                                                   |
| `lifecycle`                   | Allows you to add lifecycle configuration. See [values.yaml](https://github.com/elastic/helm-charts/tree/master/elasticsearch/values.yaml) for an example of the formatting.                                                                                                                                                | `{}`                                                                                                                      |
| `keystore`                    | Allows you map Kubernetes secrets into the keystore. See the [config example](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples/config/values.yaml) and [how to use the keystore](https://github.com/elastic/helm-charts/tree/master/elasticsearch/README.md#how-to-use-the-keystore)               | `[]`                                                                                                                      |
| `rbac`                        | Configuration for creating a role, role binding and service account as part of this helm chart with `create: true`. Also can be used to reference an external service account with `serviceAccountName: "externalServiceAccountName"`.                                                                                      | `create: false`<br>`serviceAccountName: ""`                                                                               |
| `podSecurityPolicy`           | Configuration for create a pod security policy with minimal permissions to run this Helm chart with `create: true`. Also can be used to reference an external pod security policy with `name: "externalPodSecurityPolicy"`                                                                                                  | `create: false`<br>`name: ""`                                                                                             |
| `nameOverride`                | Overrides the clusterName when used in the naming of resources                                                                                                                                                                                                                                                              | `""`                                                                                                                      |
| `fullnameOverride`            | Overrides the clusterName and nodeGroup when used in the naming of resources. This should only be used when using a single nodeGroup, otherwise you will have name conflicts                                                                                                                                                | `""`                                                                                                                      |

## Try it out

In [examples/](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples) you will find some example configurations. These examples are used for the automated testing of this helm chart

### Default

To deploy a cluster with all default values and run the integration tests

```
cd examples/default
make
```

### Multi

A cluster with dedicated node types

```
cd examples/multi
make
```

### Security

A cluster with node to node security and https enabled. This example uses autogenerated certificates and password, for a production deployment you want to generate SSL certificates following the [official docs](https://www.elastic.co/guide/en/elasticsearch/reference/current/configuring-tls.html#node-certificates).

* Generate the certificates and install Elasticsearch
  ```
  cd examples/security
  make

  # Run a curl command to interact with the cluster
  kubectl exec -ti security-master-0 -- sh -c 'curl -u $ELASTIC_USERNAME:$ELASTIC_PASSWORD -k https://localhost:9200/_cluster/health?pretty'
  ```

### FAQ

#### How to install plugins?

The [recommended](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html#_c_customized_image) way to install plugins into our docker images is to create a custom docker image.

The Dockerfile would look something like:

```
ARG elasticsearch_version
FROM docker.elastic.co/elasticsearch/elasticsearch:${elasticsearch_version}

RUN bin/elasticsearch-plugin install --batch repository-gcs
```

And then updating the `image` in values to point to your custom image.

There are a couple reasons we recommend this.

1. Tying the availability of Elasticsearch to the download service to install plugins is not a great idea or something that we recommend. Especially in Kubernetes where it is normal and expected for a container to be moved to another host at random times.
2. Mutating the state of a running docker image (by installing plugins) goes against best practices of containers and immutable infrastructure.

#### How to use the keystore?


##### Basic example

Create the secret, the key name needs to be the keystore key path. In this example we will create a secret from a file and from a literal string.

```
kubectl create secret generic encryption_key --from-file=xpack.watcher.encryption_key=./watcher_encryption_key
kubectl create secret generic slack_hook --from-literal=xpack.notification.slack.account.monitoring.secure_url='https://hooks.slack.com/services/asdasdasd/asdasdas/asdasd'
```

To add these secrets to the keystore:
```
keystore:
  - secretName: encryption_key
  - secretName: slack_hook
```

##### Multiple keys

All keys in the secret will be added to the keystore. To create the previous example in one secret you could also do:

```
kubectl create secret generic keystore_secrets --from-file=xpack.watcher.encryption_key=./watcher_encryption_key --from-literal=xpack.notification.slack.account.monitoring.secure_url='https://hooks.slack.com/services/asdasdasd/asdasdas/asdasd'
```

```
keystore:
  - secretName: keystore_secrets
```

##### Custom paths and keys

If you are using these secrets for other applications (besides the Elasticsearch keystore) then it is also possible to specify the keystore path and which keys you want to add. Everything specified under each `keystore` item will be passed through to the `volumeMounts` section for [mounting the secret](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets). In this example we will only add the `slack_hook` key from a secret that also has other keys. Our secret looks like this:

```
kubectl create secret generic slack_secrets --from-literal=slack_channel='#general' --from-literal=slack_hook='https://hooks.slack.com/services/asdasdasd/asdasdas/asdasd'
```

We only want to add the `slack_hook` key to the keystore at path `xpack.notification.slack.account.monitoring.secure_url`.

```
keystore:
  - secretName: slack_secrets
    items:
    - key: slack_hook
      path: xpack.notification.slack.account.monitoring.secure_url
```

You can also take a look at the [config example](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples/config/) which is used as part of the automated testing pipeline.

#### How to enable snapshotting?

1. Install your [snapshot plugin](https://www.elastic.co/guide/en/elasticsearch/plugins/current/repository.html) into a custom docker image following the [how to install plugins guide](https://github.com/elastic/helm-charts/tree/master/elasticsearch/README.md#how-to-install-plugins)
2. Add any required secrets or credentials into an Elasticsearch keystore following the [how to use the keystore guide](https://github.com/elastic/helm-charts/tree/master/elasticsearch/README.md#how-to-use-the-keystore)
3. Configure the [snapshot repository](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-snapshots.html) as you normally would.
4. To automate snapshots you can use a tool like [curator](https://www.elastic.co/guide/en/elasticsearch/client/curator/current/snapshot.html). In the future there are plans to have Elasticsearch manage automated snapshots with [Snapshot Lifecycle Management](https://github.com/elastic/elasticsearch/issues/38461).

### Local development environments

This chart is designed to run on production scale Kubernetes clusters with multiple nodes, lots of memory and persistent storage. For that reason it can be a bit tricky to run them against local Kubernetes environments such as minikube. Below are some examples of how to get this working locally.

#### Minikube

This chart also works successfully on [minikube](https://kubernetes.io/docs/setup/minikube/) in addition to typical hosted Kubernetes environments.
An example `values.yaml` file for minikube is provided under `examples/`.

In order to properly support the required persistent volume claims for the Elasticsearch `StatefulSet`, the `default-storageclass` and `storage-provisioner` minikube addons must be enabled.

```
minikube addons enable default-storageclass
minikube addons enable storage-provisioner
cd examples/minikube
make
```

Note that if `helm` or `kubectl` timeouts occur, you may consider creating a minikube VM with more CPU cores or memory allocated.

#### Docker for Mac - Kubernetes

It is also possible to run this chart with the built in Kubernetes cluster that comes with [docker-for-mac](https://docs.docker.com/docker-for-mac/kubernetes/).

```
cd examples/docker-for-mac
make
```

#### KIND - Kubernetes

It is also possible to run this chart using a Kubernetes [KIND (Kubernetes in Docker)](https://github.com/kubernetes-sigs/kind) cluster:

```
cd examples/kubernetes-kind
make
```

#### MicroK8S

It is also possible to run this chart using [MicroK8S](https://microk8s.io):

```
microk8s.enable dns
microk8s.enable helm
microk8s.enable storage
cd examples/microk8s
make
```

## Clustering and Node Discovery

This chart facilitates Elasticsearch node discovery and services by creating two `Service` definitions in Kubernetes, one with the name `$clusterName-$nodeGroup` and another named `$clusterName-$nodeGroup-headless`.
Only `Ready` pods are a part of the `$clusterName-$nodeGroup` service, while all pods (`Ready` or not) are a part of `$clusterName-$nodeGroup-headless`.

If your group of master nodes has the default `nodeGroup: master` then you can just add new groups of nodes with a different `nodeGroup` and they will automatically discover the correct master. If your master nodes have a different `nodeGroup` name then you will need to set `masterService` to `$clusterName-$masterNodeGroup`.

The chart value for `masterService` is used to populate `discovery.zen.ping.unicast.hosts`, which Elasticsearch nodes will use to contact master nodes and form a cluster.
Therefore, to add a group of nodes to an existing cluster, setting `masterService` to the desired `Service` name of the related cluster is sufficient.

For an example of deploying both a group master nodes and data nodes using multiple releases of this chart, see the accompanying values files in `examples/multi`.

## Testing

This chart uses [pytest](https://docs.pytest.org/en/latest/) to test the templating logic. The dependencies for testing can be installed from the [`requirements.txt`](https://github.com/elastic/helm-charts/tree/master/requirements.txt) in the parent directory.

```
pip install -r ../requirements.txt
make pytest
```

You can also use `helm template` to look at the YAML being generated

```
make template
```

It is possible to run all of the tests and linting inside of a docker container

```
make test
```

## Integration Testing

Integration tests are run using [goss](https://github.com/aelsabbahy/goss/blob/master/docs/manual.md) which is a serverspec like tool written in golang. See [goss.yaml](https://github.com/elastic/helm-charts/tree/master/elasticsearch/examples/default/test/goss.yaml) for an example of what the tests look like.

To run the goss tests against the default example:

```
cd examples/default
make goss
```
