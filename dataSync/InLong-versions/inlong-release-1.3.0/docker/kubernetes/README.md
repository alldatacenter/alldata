# The Helm Chart for Apache InLong

## Prerequisites

- Kubernetes 1.10+
- Helm 3.0+
- A dynamic provisioner for the PersistentVolumes(`production environment`)

## Usage

### Install

If the namespace named `inlong` does not exist, create it first by running:

```shell
kubectl create namespace inlong
```

To install the chart with a namespace named `inlong`, try:

```shell
helm upgrade inlong --install -n inlong ./
```

### Access InLong Dashboard

If `ingress.enabled` in [values.yaml](values.yaml) is set to `true`, you just access `http://${ingress.host}/dashboard` in browser.

Otherwise, when `dashboard.service.type` is set to `ClusterIP`, you need to execute the port-forward command like:

```shell
export DASHBOARD_POD_NAME=$(kubectl get pods -l "app.kubernetes.io/name=inlong-dashboard,app.kubernetes.io/instance=inlong" -o jsonpath="{.items[0].metadata.name}" -n inlong)
export DASHBOARD_CONTAINER_PORT=$(kubectl get pod $DASHBOARD_POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}" -n inlong)
kubectl port-forward $DASHBOARD_POD_NAME 8181:$DASHBOARD_CONTAINER_PORT -n inlong
```

And then access [http://127.0.0.1:8181](http://127.0.0.1:8181)

> Tip: If the error of `unable to do port forwarding: socat not found` appears, you need to install `socat` at first.

Or when `dashboard.service.type` is set to `NodePort`, you need to execute the following commands:

```shell
export DASHBOARD_NODE_IP=$(kubectl get nodes -o jsonpath="{.items[0].status.addresses[0].address}" -n inlong)
export DASHBOARD_NODE_PORT=$(kubectl get svc inlong-dashboard -o jsonpath="{.spec.ports[0].nodePort}" -n inlong)
```

And then access `http://$DASHBOARD_NODE_IP:$DASHBOARD_NODE_PORT`

When `dashboard.service.type` is set to `LoadBalancer`, you need to execute the following command:

```shell
export DASHBOARD_SERVICE_IP=$(kubectl get svc inlong-dashboard --template "{{"{{ range (index .status.loadBalancer.ingress 0) }}{{.}}{{ end }}"}}"  -n inlong)
```

And then access `http://$DASHBOARD_SERVICE_IP:30080`

> NOTE: It may take a few minutes for the `LoadBalancer` IP to be available. You can check the status by running `kubectl get svc inlong-dashboard -n inlong -w`

The default username is `admin` and the default password is `inlong`. You can access the InLong Dashboard through them.

### Configuration

The configuration file is [values.yaml](values.yaml), and the following tables lists the configurable parameters of InLong and their default values.

|                                    Parameter                                     |     Default      |                                                                         Description                                                                          | 
|:--------------------------------------------------------------------------------:|:----------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|                                    `timezone`                                    | `Asia/Shanghai`  |                                                       World time and date for cities in all time zones                                                       |
|                               `images.pullPolicy`                                |  `IfNotPresent`  |                                                 Image pull policy. One of `Always`, `Never`, `IfNotPresent`                                                  |
|                         `images.<component>.repository`                          |                  |                                                          Docker image repository for the component                                                           |
|                             `images.<component>.tag`                             |     `latest`     |                                                              Docker image tag for the component                                                              |
|                             `<component>.component`                              |                  |                                                                        Component name                                                                        |
|                              `<component>.replicas`                              |       `1`        |                                                Replicas is the desired number of replicas of a given Template                                                |
|                        `<component>.podManagementPolicy`                         |  `OrderedReady`  |                PodManagementPolicy controls how pods are created during initial scale up, when replacing pods on nodes, or when scaling down                 |
|                            `<component>.annotations`                             |       `{}`       |                                 The `annotations` field can be used to attach arbitrary non-identifying metadata to objects                                  |
|                            `<component>.tolerations`                             |       `[]`       |                     Tolerations are applied to pods, and allow (but do not require) the pods to schedule onto nodes with matching taints                     |
|                            `<component>.nodeSelector`                            |       `{}`       |                 You can add the `nodeSelector` field to your Pod specification and specify the node labels you want the target node to have                  |
|                              `<component>.affinity`                              |       `{}`       |        Node affinity is conceptually similar to nodeSelector, allowing you to constrain which nodes your Pod can be scheduled on based on node labels        |
|                   `<component>.terminationGracePeriodSeconds`                    |       `30`       |                                              Optional duration in seconds the pod needs to terminate gracefully                                              |
|                             `<component>.resources`                              |       `{}`       |                                                Optionally specify how much of each resource a container needs                                                |
|                              `<component>.port(s)`                               |                  |                                                            The port(s) for each component service                                                            |
|                                `<component>.env`                                 |       `{}`       |                                                      Environment variables for each component container                                                      |
|       <code>\<component\>.probe.\<liveness&#124;readiness\>.enabled</code>       |      `true`      |                                                         Turn on and off liveness or readiness probe                                                          |
|  <code>\<component\>.probe.\<liveness&#124;readiness\>.failureThreshold</code>   |       `10`       |                                                         Minimum consecutive successes for the probe                                                          |
| <code>\<component\>.probe.\<liveness&#124;readiness\>.initialDelaySeconds</code> |       `10`       |                                                             Delay before the probe is initiated                                                              |
|    <code>\<component\>.probe.\<liveness&#124;readiness\>.periodSeconds</code>    |       `30`       |                                                                How often to perform the probe                                                                |
|                            `<component>.volumes.name`                            |                  |                                                                         Volume name                                                                          |
|                            `<component>.volumes.size`                            |      `10Gi`      |                                                                         Volume size                                                                          |
|                        `<component>.service.annotations`                         |       `{}`       |                                        The `annotations` field may need to be set when service.type is `LoadBalancer`                                        |
|                            `<component>.service.type`                            |   `ClusterIP`    |             The `type` field determines how the service is exposed. Valid options are `ClusterIP`, `NodePort`, `LoadBalancer` and `ExternalName`             |
|                         `<component>.service.clusterIP`                          |      `nil`       |                                  ClusterIP is the IP address of the service and is usually assigned randomly by the master                                   |
|                          `<component>.service.nodePort`                          |      `nil`       |                              NodePort is the port on each node on which this service is exposed when service type is `NodePort`                              |
|                       `<component>.service.loadBalancerIP`                       |      `nil`       |                            LoadBalancer will get created with the IP specified in this field when service type is `LoadBalancer`                             |
|                        `<component>.service.externalName`                        |      `nil`       | ExternalName is the external reference that kubedns or equivalent will return as a CNAME record for this service, requires service type to be `ExternalName` |
|                        `<component>.service.externalIPs`                         |       `[]`       |                        ExternalIPs is a list of IP addresses for which nodes in the cluster will also accept traffic for this service                        |
|                             `external.mysql.enabled`                             |     `false`      |                                         If not exists external MySQL, InLong will use the internal MySQL by default                                          |
|                            `external.mysql.hostname`                             |   `localhost`    |                                                                   External MySQL hostname                                                                    |
|                              `external.mysql.port`                               |      `3306`      |                                                                     External MySQL port                                                                      |
|                            `external.mysql.username`                             |      `root`      |                                                                   External MySQL username                                                                    |
|                            `external.mysql.password`                             |    `password`    |                                                                   External MySQL password                                                                    |
|                            `external.pulsar.enabled`                             |     `false`      |                                        If not exists external Pulsar, InLong will use the internal TubeMQ by default                                         |
|                           `external.pulsar.serviceUrl`                           | `localhost:6650` |                                                                 External Pulsar service URL                                                                  |
|                            `external.pulsar.adminUrl`                            | `localhost:8080` |                                                                  External Pulsar admin URL                                                                   |

> The optional components include `agent`, `audit`, `dashboard`, `dataproxy`, `manager`, `tubemq-manager`, `tubemq-master`, `tubemq-broker`, `zookeeper` and `mysql`.

### Uninstall

To uninstall the release, try:

```shell
helm uninstall inlong -n inlong
```

The above command removes all the Kubernetes components except the `PVC` associated with the chart, and deletes the release.
You can delete all `PVC` if any persistent volume claims used, it will lose all data.

```shell
kubectl delete pvc -n inlong --all
```

> Note: Deleting the PVC also delete all data. Please be cautious before doing it.

## Development

A Kubernetes cluster with [helm](https://helm.sh) is required before development.
But it doesn't matter if you don't have one, the [kind](https://github.com/kubernetes-sigs/kind) is recommended.
It runs a local Kubernetes cluster in Docker container. Therefore, it requires very little time to up and stop the Kubernetes node.

### Quick start with kind

You can install kind by following the [Quick Start](https://kind.sigs.k8s.io/docs/user/quick-start) section of their official documentation.

After installing kind, you can create a Kubernetes cluster with the [configuration file](../../.github/kind.yml).
Try in the project root:

```shell
kind create cluster --config .github/kind.yml
```

To specify another image use the `--image` flag – `kind create cluster --image=....`.
Using a different image allows you to change the Kubernetes version of the created cluster.
To find images suitable for a given release currently you should check the [release notes](https://github.com/kubernetes-sigs/kind/releases) for your given kind version (check with `kind version`) where you'll find a complete listing of images created for a kind release.

After installing kind, you can interact with the created cluster, try:

```shell
kubectl cluster-info --context kind-inlong-cluster
```

Now, you have a running Kubernetes cluster for local development.

### Install Helm

Please follow the [installation guide](https://helm.sh/docs/intro/install) in the official documentation to install Helm.

### Install the chart

To create the namespace and install the chart, in the [docker/kubernetes](.) folder, try:

```shell
kubectl create namespace inlong
helm upgrade inlong --install -n inlong ./
```

It may take a few minutes. Confirm the pods are up:

```shell
watch kubectl get pods -n inlong -o wide
```

### Develop and debug

Follow the [template debugging guide](https://helm.sh/docs/chart_template_guide/debugging) in the official documentation to debug your chart.

Besides, you can save the rendered templates by:

```shell
helm template ./ --output-dir ./result
```

Then, you can check the rendered templates in the `result` directory.

#### Quick start with Chart Testing

Also, you can use the [helm/chart-testing](https://github.com/helm/chart-testing) tool to test your Helm chart.
You can follow the [installation](https://github.com/helm/chart-testing#installation) section in the official documentation to install it.
But for simplicity, their [Docker image](https://quay.io/repository/helmpack/chart-testing?tab=tags) is recommended.

To list changed charts, in the project root, try:

```shell
docker run -it --rm \
               --name ct \
               --workdir=/data \
               --volume "$(pwd)":/data \
               quay.io/helmpack/chart-testing \
               ct list-changed --chart-dirs docker --target-branch master
```

To lint and validate the chart, try:

```shell
docker run -it --rm \
               --name ct \
               --workdir=/data \
               --volume "$(pwd)":/data \
               quay.io/helmpack/chart-testing \
               ct lint --chart-dirs docker
```

To install and test the chart, try:

```shell
docker run -it --rm \
               --name ct \
               --workdir=/data \
               --volume "$(pwd)":/data \
               quay.io/helmpack/chart-testing \
               ct install --chart-dirs docker
```

> NOTE: If the charts have not changed, they will not be linted, validated, installed and tested.

## Contribution

When you decide to contribute, you should follow these steps:

1. Develop the chart locally and verify your changes.
2. Don't forget to bump up the version in [Chart.yaml](Chart.yaml).
3. Feel free to make a [Pull Request](https://github.com/apache/inlong/compare)!
4. Finally, you can view the status of the [lint and test workflow](https://github.com/apache/inlong/actions/workflows/ci_chart_test.yml).
   > In this workflow, changes between your branch and the master branch will be checked and listed.
   > All changed charts will be linted, validated, installed and tested.

## Troubleshooting

We've done our best to make these charts as seamless as possible, but occasionally there are circumstances beyond our control.
We've collected tips and tricks for troubleshooting common issues.
Please examine these first before raising an [issue](https://github.com/apache/inlong/issues/new/choose), and feel free to make a [Pull Request](https://github.com/apache/inlong/compare)!
