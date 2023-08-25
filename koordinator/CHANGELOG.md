# Change Log


## [v0.4.0](https://github.com/koordinator-sh/koordinator/tree/v0.4.0) (2022-05-31)
[Full Changelog](https://github.com/koordinator-sh/koordinator/compare/v0.3.1...v0.4.0)

**Features and improvements:**

- Introduce koord-runtime-proxy [\#171](https://github.com/koordinator-sh/koordinator/pull/171)
- koord-runtime-proxy supports docker proxy [\#64](https://github.com/koordinator-sh/koordinator/issues/64)
- Support Memory eviction lower percent [\#85](https://github.com/koordinator-sh/koordinator/issues/85)
- Support load-aware scheduling [\#135](https://github.com/koordinator-sh/koordinator/pull/135)
- Support BE Pods eviction based on satisfaction [\#147](https://github.com/koordinator-sh/koordinator/issues/147)
- Support group identity [\#154](https://github.com/koordinator-sh/koordinator/issues/154)

**Fixed bugs:**

- Use the limit as request for BE Pods [\#49](https://github.com/koordinator-sh/koordinator/pull/129)

**Merged pull requests:**

- fix be container memory request [\#129](https://github.com/koordinator-sh/koordinator/pull/129) ([shinytang6](https://github.com/shinytang6))
- add koordlet runtime design [\#123](https://github.com/koordinator-sh/koordinator/pull/123) ([zwzhang0107](https://github.com/zwzhang0107))
- support memoryEvictLowerPercent in memory evict [\#132](https://github.com/koordinator-sh/koordinator/pull/132) ([shinytang6](https://github.com/shinytang6))
- add validation for CRD [\#133](https://github.com/koordinator-sh/koordinator/pull/133) ([jasonliu747](https://github.com/jasonliu747))
- Modify memqos wmark ratio doc desc [\#142](https://github.com/koordinator-sh/koordinator/pull/142) ([tianzichenone](https://github.com/tianzichenone))
- proposal load-aware scheduling plugin [\#135](https://github.com/koordinator-sh/koordinator/pull/135) ([eahydra](https://github.com/eahydra))
- koordlet: support NodeMetricCollectPolicy [\#157](https://github.com/koordinator-sh/koordinator/pull/157) ([eahydra](https://github.com/eahydra))
- koord-scheduler: Support load aware scheduling [\#159](https://github.com/koordinator-sh/koordinator/pull/159) ([eahydra](https://github.com/eahydra))
- koordlet: support collect BE CPU metric [\#158](https://github.com/koordinator-sh/koordinator/pull/158) ([jasonliu747](https://github.com/jasonliu747))
- apis: introduce cpu evict fields in NodeSLO [\#161](https://github.com/koordinator-sh/koordinator/pull/161) ([jasonliu747](https://github.com/jasonliu747))
- koordlet: support cpu evict feature [\#169](https://github.com/koordinator-sh/koordinator/pull/169) ([jasonliu747](https://github.com/jasonliu747))
- Add pod annotations/labels for container level hook [\#165](https://github.com/koordinator-sh/koordinator/pull/165) ([honpey](https://github.com/honpey))
- Introduce image service proxy under cri scenario [\#168](https://github.com/koordinator-sh/koordinator/pull/168) ([honpey](https://github.com/honpey))
- koord-runtime-proxy: refactor codes about store and resource-exectutor [\#170](https://github.com/koordinator-sh/koordinator/pull/170) ([honpey](https://github.com/honpey))
- Introduce main for koord-runtime-proxy [\#171](https://github.com/koordinator-sh/koordinator/pull/171) ([honpey](https://github.com/honpey))
- Add the koord-runtime-proxy design doc [\#178](https://github.com/koordinator-sh/koordinator/pull/178) ([honpey](https://github.com/honpey))
- koord-runtime-proxy supports docker proxy [\#128](https://github.com/koordinator-sh/koordinator/pull/128) ([ZYecho](https://github.com/ZYecho))
- add group identity plugin [\#166](https://github.com/koordinator-sh/koordinator/pull/166) ([zwzhang0107](https://github.com/zwzhang0107))
- use T.TempDir to create temporary test directory [\#151](https://github.com/koordinator-sh/koordinator/pull/151) ([Juneezee](https://github.com/Juneezee))
- update codecov configuration [\#131](https://github.com/koordinator-sh/koordinator/pull/131) ([saintube](https://github.com/saintube))

**New Contributors**

- [shinytang6](https://github.com/shinytang6) made their contributions in [\#129](https://github.com/koordinator-sh/koordinator/pull/129), [\#132](https://github.com/koordinator-sh/koordinator/pull/132)
- [tianzichenone](https://github.com/tianzichenone) made their first contribution in [\#142](https://github.com/koordinator-sh/koordinator/pull/142)
- [Juneezee](https://github.com/Juneezee) made their first contribution in [\#151](https://github.com/koordinator-sh/koordinator/pull/151)
- [ZYecho](https://github.com/ZYecho) made their first contribution in [\#128](https://github.com/koordinator-sh/koordinator/pull/128)


## [v0.3.0](https://github.com/koordinator-sh/koordinator/tree/v0.3.0) (2022-05-07)
[Full Changelog](https://github.com/koordinator-sh/koordinator/compare/v0.2.0...v0.3.0)

**Features and improvements:**

- Support CPU burst strategy [\#52](https://github.com/koordinator-sh/koordinator/issues/52)
- Support Memory QoS strategy [\#55](https://github.com/koordinator-sh/koordinator/issues/55)
- Support LLC and MBA isolation strategy [\#56](https://github.com/koordinator-sh/koordinator/issues/56)
- Protocol design between runtime-manager and hook server [\#62](https://github.com/koordinator-sh/koordinator/issues/62)
- Improve overall code coverage from 39% to 56% [\#69](https://github.com/koordinator-sh/koordinator/issues/69)

**Fixed bugs:**

- when deploy on ACK 1.18.1 koord-manager pod always crash [\#49](https://github.com/koordinator-sh/koordinator/issues/49)
- Handle unexpected CPU info in case of koordlet panic [\#90](https://github.com/koordinator-sh/koordinator/issues/90)

**Merged pull requests:**

- New feature: cpu burst strategy [\#73](https://github.com/koordinator-sh/koordinator/pull/73) ([stormgbs](https://github.com/stormgbs))
- Introduce protocol between RuntimeManager and RuntimeHookServer [\#76](https://github.com/koordinator-sh/koordinator/pull/76) ([honpey](https://github.com/honpey))
- Improve readme [\#88](https://github.com/koordinator-sh/koordinator/pull/88) ([hormes](https://github.com/hormes))
- update image file format [\#92](https://github.com/koordinator-sh/koordinator/pull/92) ([zwzhang0107](https://github.com/zwzhang0107))
- 🌱 add expire cache [\#93](https://github.com/koordinator-sh/koordinator/pull/93) ([jasonliu747](https://github.com/jasonliu747))
- ✨ support LLC & MBA isolation [\#94](https://github.com/koordinator-sh/koordinator/pull/94) ([jasonliu747](https://github.com/jasonliu747))
- fix cpuinfo panic on arm64 [\#97](https://github.com/koordinator-sh/koordinator/pull/97) ([saintube](https://github.com/saintube))
- 📖 fix typo in docs [\#98](https://github.com/koordinator-sh/koordinator/pull/98) ([jasonliu747](https://github.com/jasonliu747))
- Introduce HookServer config loading from /etc/runtime/hookserver.d/ [\#100](https://github.com/koordinator-sh/koordinator/pull/100) ([honpey](https://github.com/honpey))
- add memory qos strategy [\#101](https://github.com/koordinator-sh/koordinator/pull/101) ([saintube](https://github.com/saintube))
- add an issue template and rename feature request to proposal [\#108](https://github.com/koordinator-sh/koordinator/pull/108) ([hormes](https://github.com/hormes))
- Introduce cri request parsing/generate-hook-request/checkpoing logic [\#110](https://github.com/koordinator-sh/koordinator/pull/110) ([honpey](https://github.com/honpey))
- 🌱 add unit test for resmanager [\#111](https://github.com/koordinator-sh/koordinator/pull/111) ([jasonliu747](https://github.com/jasonliu747))
- Add cpu suppress test and revise memory qos [\#112](https://github.com/koordinator-sh/koordinator/pull/112) ([saintube](https://github.com/saintube))
- ✨ Remove deprecated go get from Makefile [\#116](https://github.com/koordinator-sh/koordinator/pull/116) ([jasonliu747](https://github.com/jasonliu747))
- 🌱 add license checker in workflow [\#117](https://github.com/koordinator-sh/koordinator/pull/117) ([jasonliu747](https://github.com/jasonliu747))
- Support cpu burst strategy [\#118](https://github.com/koordinator-sh/koordinator/pull/118) ([stormgbs](https://github.com/stormgbs))
- 🌱 add unit test for memory evict feature [\#119](https://github.com/koordinator-sh/koordinator/pull/119) ([jasonliu747](https://github.com/jasonliu747))
- add UTs for runtime handler [\#125](https://github.com/koordinator-sh/koordinator/pull/125) ([saintube](https://github.com/saintube))
- 📖 add changelog for v0.3 [\#126](https://github.com/koordinator-sh/koordinator/pull/126) ([jasonliu747](https://github.com/jasonliu747))

**New Contributors**

- [honpey](https://github.com/honpey) made their first contribution in [\#76](https://github.com/koordinator-sh/koordinator/pull/76)
- [saintube](https://github.com/saintube) made their first contribution in [\#97](https://github.com/koordinator-sh/koordinator/pull/97)

## v0.2.0

## Isolate resources for best-effort workloads

In Koodinator v0.2.0, we refined the ability to isolate resources for best-effort workloads.

`koordlet` will set the cgroup parameters according to the resources described in the Pod Spec. Currently, supports
setting CPU Request/Limit, and Memory Limit.

For CPU resources, only the case of `request == limit` is supported, and the support for the scenario
of `request <= limit` will be supported in the next version.

## Active eviction mechanism based on memory safety thresholds

When latency-sensitive applications are serving, memory usage may increase due to burst traffic. Similarly, there may be
similar scenarios for best-effort workloads, for example, the current computing load exceeds the expected resource
Request/Limit.

These scenarios will lead to an increase in the overall memory usage of the node, which will have an unpredictable
impact on the runtime stability of the node side. For example, it can reduce the quality of service of latency-sensitive
applications or even become unavailable. Especially in a co-location environment, it is more challenging.

We implemented an active eviction mechanism based on memory safety thresholds in Koodinator.

`koordlet` will regularly check the recent memory usage of node and Pods to check whether the safety threshold is
exceeded. If it exceeds, it will evict some best-effort Pods to release memory. This mechanism can better ensure the
stability of node and latency-sensitive applications.

`koordlet` currently only evicts best-effort Pods, sorted according to the Priority specified in the Pod Spec. The lower
the priority, the higher the priority to be evicted, the same priority will be sorted according to the memory usage
rate (RSS), the higher the memory usage, the higher the priority to be evicted. This eviction selection algorithm is not
static. More dimensions will be considered in the future, and more refined implementations will be implemented for more
scenarios to achieve more reasonable evictions.

The current memory utilization safety threshold default value is 70%. You can modify the `memoryEvictThresholdPercent`
in ConfigMap `slo-controller-config` according to the actual situation,

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: slo-controller-config
  namespace: koordinator-system
data:
  colocation-config: |
    {
      "enable": true
    }
  resource-threshold-config: |
    {
      "clusterStrategy": {
        "enable": true,
        "memoryEvictThresholdPercent": 70
      }
    }
```

## v0.1.0

### Node Metrics

Koordinator defines the `NodeMetrics` CRD, which is used to record the resource utilization of a single node and all
Pods on the node. koordlet will regularly report and update `NodeMetrics`. You can view `NodeMetrics` with the following
commands.

```shell
$ kubectl get nodemetrics node-1 -o yaml
apiVersion: slo.koordinator.sh/v1alpha1
kind: NodeMetric
metadata:
  creationTimestamp: "2022-03-30T11:50:17Z"
  generation: 1
  name: node-1
  resourceVersion: "2687986"
  uid: 1567bb4b-87a7-4273-a8fd-f44125c62b80
spec: {}
status:
  nodeMetric:
    nodeUsage:
      resources:
        cpu: 138m
        memory: "1815637738"
  podsMetric:
  - name: storage-service-6c7c59f868-k72r5
    namespace: default
    podUsage:
      resources:
        cpu: "300m"
        memory: 17828Ki
```

### Colocation Resources

After the Koordinator is deployed in the K8s cluster, the Koordinator will calculate the CPU and Memory resources that
have been allocated but not used according to the data of `NodeMetrics`. These resources are updated in Node in the form
of extended resources.

`koordinator.sh/batch-cpu` represents the CPU resources for Best Effort workloads,
`koordinator.sh/batch-memory` represents the Memory resources for Best Effort workloads.

You can view these resources with the following commands.

```shell
$ kubectl describe node node-1
Name:               node-1
....
Capacity:
  cpu:                          8
  ephemeral-storage:            103080204Ki
  koordinator.sh/batch-cpu:     4541
  koordinator.sh/batch-memory:  17236565027
  memory:                       32611012Ki
  pods:                         64
Allocatable:
  cpu:                          7800m
  ephemeral-storage:            94998715850
  koordinator.sh/batch-cpu:     4541
  koordinator.sh/batch-memory:  17236565027
  memory:                       28629700Ki
  pods:                         64
```

### Cluster-level Colocation Profile

In order to make it easier for everyone to use Koordinator to co-locate different workloads, we
defined `ClusterColocationProfile` to help gray workloads use co-location resources. A `ClusterColocationProfile` is CRD
like the one below. Please do edit each parameter to fit your own use cases.

```yaml
apiVersion: config.koordinator.sh/v1alpha1
kind: ClusterColocationProfile
metadata:
  name: colocation-profile-example
spec:
  namespaceSelector:
    matchLabels:
      koordinator.sh/enable-colocation: "true"
  selector:
    matchLabels:
      sparkoperator.k8s.io/launched-by-spark-operator: "true"
  qosClass: BE
  priorityClassName: koord-batch
  koordinatorPriority: 1000
  schedulerName: koord-scheduler
  labels:
    koordinator.sh/mutated: "true"
  annotations:
    koordinator.sh/intercepted: "true"
  patch:
    spec:
      terminationGracePeriodSeconds: 30
```

Various Koordinator components ensure scheduling and runtime quality through labels `koordinator.sh/qosClass`
, `koordinator.sh/priority` and kubernetes native priority.

With the webhook mutating mechanism provided by Kubernetes, koord-manager will modify Pod resource requirements to
co-located resources, and inject the QoS and Priority defined by Koordinator into Pod.

Taking the above Profile as an example, when the Spark Operator creates a new Pod in the namespace with
the `koordinator.sh/enable-colocation=true` label, the Koordinator QoS label `koordinator.sh/qosClass` will be injected
into the Pod. According to the Profile definition PriorityClassName, modify the Pod's PriorityClassName and the
corresponding Priority value. Users can also set the Koordinator Priority according to their needs to achieve more
fine-grained priority management, so the Koordinator Priority label `koordinator.sh/priority` is also injected into the
Pod. Koordinator provides an enhanced scheduler koord-scheduler, so you need to modify the Pod's scheduler name
koord-scheduler through Profile.

If you expect to integrate Koordinator into your own system, please learn more about
the [core concepts](/docs/core-concepts/architecture).

### CPU Suppress

In order to ensure the runtime quality of different workloads in co-located scenarios, Koordinator uses the CPU Suppress
mechanism provided by koordlet on the node side to suppress workloads of the Best Effort type when the load increases.
Or increase the resource quota for Best Effort type workloads when the load decreases.

When installing through the helm chart, the ConfigMap `slo-controller-config` will be created in the koordinator-system
namespace, and the CPU Suppress mechanism is enabled by default. If it needs to be closed, refer to the configuration
below, and modify the configuration of the resource-threshold-config section to take effect.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: slo-controller-config
  namespace: {{.Values.installation.namespace}}
data:
  ...
  resource-threshold-config: |
    {
      "clusterStrategy": {
        "enable": false
      }
    }
```

### Colocation Resources Balance

Koordinator currently adopts a strategy for node co-location resource scheduling, which prioritizes scheduling to
machine with more resources remaining in co-location to avoid Best Effort workloads crowding together. More rich
scheduling capabilities are on the way.
