# Filebeat Helm Chart

This Helm chart is a lightweight way to configure and run our official
[Filebeat Docker image][].

<!-- development warning placeholder -->

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Requirements](#requirements)
- [Installing](#installing)
  - [Install released version using Helm repository](#install-released-version-using-helm-repository)
  - [Install development version from a branch](#install-development-version-from-a-branch)
- [Upgrading](#upgrading)
- [Usage notes](#usage-notes)
- [Configuration](#configuration)
- [FAQ](#faq)
  - [How to use Filebeat with Elasticsearch with security (authentication and TLS) enabled?](#how-to-use-filebeat-with-elasticsearch-with-security-authentication-and-tls-enabled)
  - [How to install OSS version of Filebeat?](#how-to-install-oss-version-of-filebeat)
  - [Why is Filebeat host.name field set to Kubernetes pod name?](#why-is-filebeat-hostname-field-set-to-kubernetes-pod-name)
  - [How do I get multiple beats agents working with hostNetworking enabled?](#how-do-i-get-multiple-beats-agents-working-with-hostnetworking-enabled)
  - [How to change readinessProbe for outputs which don't support testing](#how-to-change-readinessprobe-for-outputs-which-dont-support-testing)
- [Contributing](#contributing)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->
<!-- Use this to update TOC: -->
<!-- docker run --rm -it -v $(pwd):/usr/src jorgeandrada/doctoc --github -->


## Requirements

* Kubernetes >= 1.14
* [Helm][] >= 2.17.0

See [supported configurations][] for more details.


## Installing

This chart is tested with the latest 7.10.2 version.

### Install released version using Helm repository

* Add the Elastic Helm charts repo:
`helm repo add elastic https://helm.elastic.co`

* Install it:
  - with Helm 3: `helm install filebeat --version <version> elastic/filebeat`
  - with Helm 2 (deprecated): `helm install --name filebeat --version <version> elastic/filebeat`

### Install development version from a branch

* Clone the git repo: `git clone git@github.com:elastic/helm-charts.git`

* Checkout the branch : `git checkout 7.10`

* Install it:
  - with Helm 3: `helm install filebeat ./helm-charts/filebeat --set imageTag=7.10.2`
  - with Helm 2 (deprecated): `helm install --name filebeat ./helm-charts/filebeat --set imageTag=7.10.2`


## Upgrading

Please always check [CHANGELOG.md][] and [BREAKING_CHANGES.md][] before
upgrading to a new chart version.


## Usage notes

* The default Filebeat configuration file for this chart is configured to use an
Elasticsearch endpoint. Without any additional changes, Filebeat will send
documents to the service URL that the Elasticsearch Helm chart sets up by
default. You may either set the `ELASTICSEARCH_HOSTS` environment variable in
`extraEnvs` to override this endpoint or modify the default `filebeatConfig` to
change this behavior.
* The default Filebeat configuration file is also configured to capture
container logs and enrich them with Kubernetes metadata by default. This will
capture all container logs in the cluster.
* This chart disables the [HostNetwork][] setting by default for compatibility
reasons with the majority of kubernetes providers and scenarios. Some kubernetes
providers may not allow enabling `hostNetwork` and deploying multiple Filebeat
pods on the same node isn't possible with `hostNetwork` However Filebeat does
recommend activating it. If your kubernetes provider is compatible with
`hostNetwork` and you don't need to run multiple Filebeat DaemonSets, you can
activate it by setting `hostNetworking: true` in [values.yaml][].
* This repo includes a number of [examples][] configurations which can be used
as a reference. They are also used in the automated testing of this chart.


## Configuration

| Parameter                   | Description                                                                                                                                                                     | Default                            |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| `affinity`                  | Configurable [affinity][]                                                                                                                                                       | `{}`                               |
| `dnsConfig`                 | Configurable [dnsConfig][]                                                                                                                                                      | `{}`                               |
| `envFrom`                   | Templatable string of envFrom to be passed to the [environment from variables][] which will be appended to the `envFrom:` definition for the container                          | `[]`                               |
| `extraContainers`           | List of additional init containers to be added at the DaemonSet                                                                                                                 | `""`                               |
| `extraEnvs`                 | Extra [environment variables][] which will be appended to the `env:` definition for the container                                                                               | `[]`                               |
| `extraInitContainers`       | List of additional init containers to be added at the DaemonSet. It also accepts a templatable string of additional containers to be passed to the `tpl` function               | `[]`                               |
| `extraVolumeMounts`         | List of additional volumeMounts to be mounted on the DaemonSet                                                                                                                  | `[]`                               |
| `extraVolumes`              | List of additional volumes to be mounted on the DaemonSet                                                                                                                       | `[]`                               |
| `filebeatConfig`            | Allows you to add any config files in `/usr/share/filebeat` such as `filebeat.yml`                                                                                              | see [values.yaml][]                |
| `fullnameOverride`          | Overrides the full name of the resources. If not set the name will default to " `.Release.Name` - `.Values.nameOverride or .Chart.Name` "                                       | `""`                               |
| `hostAliases`               | Configurable [hostAliases][]                                                                                                                                                    | `[]`                               |
| `hostNetworking`            | Use host networking in the DaemonSet so that hostname is reported correctly                                                                                                     | `false`                            |
| `hostPathRoot`              | Fully-qualified [hostPath][] that will be used to persist Filebeat registry data                                                                                                | `/var/lib`                         |
| `imagePullPolicy`           | The Kubernetes [imagePullPolicy][] value                                                                                                                                        | `IfNotPresent`                     |
| `imagePullSecrets`          | Configuration for [imagePullSecrets][] so that you can use a private registry for your image                                                                                    | `[]`                               |
| `imageTag`                  | The Filebeat Docker image tag                                                                                                                                                   | `7.10.2`                  |
| `image`                     | The Filebeat Docker image                                                                                                                                                       | `docker.elastic.co/beats/filebeat` |
| `labels`                    | Configurable [labels][] applied to all Filebeat pods                                                                                                                            | `{}`                               |
| `livenessProbe`             | Parameters to pass to liveness [probe][] checks for values such as timeouts and thresholds                                                                                      | see [values.yaml][]                |
| `managedServiceAccount`     | Whether the `serviceAccount` should be managed by this Helm chart. Set this to `false` in order to manage your own service account and related roles                            | `true`                             |
| `nameOverride`              | Overrides the chart name for resources. If not set the name will default to `.Chart.Name`                                                                                       | `""`                               |
| `nodeSelector`              | Configurable [nodeSelector][]                                                                                                                                                   | `{}`                               |
| `podAnnotations`            | Configurable [annotations][] applied to all Filebeat pods                                                                                                                       | `{}`                               |
| `podSecurityContext`        | Configurable [podSecurityContext][] for Filebeat pod execution environment                                                                                                      | see [values.yaml][]                |
| `priorityClassName`         | The name of the [PriorityClass][]. No default is supplied as the PriorityClass must be created first                                                                            | `""`                               |
| `readinessProbe`            | Parameters to pass to readiness [probe][] checks for values such as timeouts and thresholds                                                                                     | see [values.yaml][]                |
| `resources`                 | Allows you to set the [resources][] for the `DaemonSet`                                                                                                                         | see [values.yaml][]                |
| `secretMounts`              | Allows you easily mount a secret as a file inside the `DaemonSet`. Useful for mounting certificates and other secrets. See [values.yaml][] for an example                       | `[]`                               |
| `serviceAccount`            | Custom [serviceAccount][] that Filebeat will use during execution. By default will use the service account created by this chart                                                | `""`                               |
| `serviceAccountAnnotations` | Annotations to be added to the ServiceAccount that is created by this chart.                                                                                                    | `{}`                               |
| `terminationGracePeriod`    | Termination period (in seconds) to wait before killing Filebeat pod process on pod shutdown                                                                                     | `30`                               |
| `tolerations`               | Configurable [tolerations][]                                                                                                                                                    | `[]`                               |
| `updateStrategy`            | The [updateStrategy][] for the `DaemonSet`. By default Kubernetes will kill and recreate pods on updates. Setting this to `OnDelete` will require that pods be deleted manually | `RollingUpdate`                    |


## FAQ

### How to use Filebeat with Elasticsearch with security (authentication and TLS) enabled?

This Helm chart can use existing [Kubernetes secrets][] to setup
credentials or certificates for examples. These secrets should be created
outside of this chart and accessed using [environment variables][] and volumes.

An example can be found in [examples/security][].

### How to install OSS version of Filebeat?

Deploying OSS version of Elasticsearch can be done by setting `image` value to
[Filebeat OSS Docker image][]

An example of Filebeat deployment using OSS version can be found in
[examples/oss][].

### Why is Filebeat host.name field set to Kubernetes pod name?

The default Filebeat configuration is using Filebeat pod name for
`agent.hostname` and `host.name` fields. The `hostname` of the Kubernetes nodes
can be find in `kubernetes.node.name` field. If you would like to have
`agent.hostname` and `host.name` fields set to the hostname of the nodes, you'll
need to set `hostNetworking` value to true.

Note that enabling [hostNetwork][] make Filebeat pod use the host network
namespace which gives it access to the host loopback device, services listening
on localhost, could be used to snoop on network activity of other pods on the
same node.

### How do I get multiple beats agents working with hostNetworking enabled?

The default http port for multiple beats agents may be on the same port, for
example, Filebeats and Metricbeats both default to 5066. When `hostNetworking`
is enabled this will cause collisions when standing up the http server. The work
around for this is to set `http.port` in the config file for one of the beats agent
to use a different port.

### How to change readinessProbe for outputs which don't support testing

Some [Filebeat outputs][] like [Kafka output][] don't support testing using
`filebeat test output` command which is used by Filebeat chart readiness probe.

This makes Filebeat pods crash before being ready with the following message:
`Readiness probe failed: kafka output doesn't support testing`.

The workaround when using this kind of output is to override the readiness probe
command to check Filebeat API instead (same as existing liveness probe).

```
readinessProbe:
  exec:
    command:
      - sh
      - -c
      - |
        #!/usr/bin/env bash -e
        curl --fail 127.0.0.1:5066
```


## Contributing

Please check [CONTRIBUTING.md][] before any contribution or for any questions
about our development and testing process.

[7.10]: https://github.com/elastic/helm-charts/releases
[BREAKING_CHANGES.md]: https://github.com/elastic/helm-charts/blob/master/BREAKING_CHANGES.md
[CHANGELOG.md]: https://github.com/elastic/helm-charts/blob/master/CHANGELOG.md
[CONTRIBUTING.md]: https://github.com/elastic/helm-charts/blob/master/CONTRIBUTING.md
[affinity]: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity
[annotations]: https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/
[dnsConfig]: https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/
[environment variables]: https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/#using-environment-variables-inside-of-your-config
[environment from variables]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#configure-all-key-value-pairs-in-a-configmap-as-container-environment-variables
[examples]: https://github.com/elastic/helm-charts/tree/7.10/filebeat/examples
[examples/oss]: https://github.com/elastic/helm-charts/tree/7.10/filebeat/examples/oss
[examples/security]: https://github.com/elastic/helm-charts/tree/7.10/filebeat/examples/security
[filebeat docker image]: https://www.elastic.co/guide/en/beats/filebeat/7.10/running-on-docker.html
[filebeat oss docker image]: https://www.docker.elastic.co/r/beats/filebeat-oss
[filebeat outputs]: https://www.elastic.co/guide/en/beats/filebeat/7.10/configuring-output.html
[helm]: https://helm.sh
[hostAliases]: https://kubernetes.io/docs/concepts/services-networking/add-entries-to-pod-etc-hosts-with-host-aliases/
[hostNetwork]: https://kubernetes.io/docs/concepts/policy/pod-security-policy/#host-namespaces
[hostPath]: https://kubernetes.io/docs/concepts/storage/volumes/#hostpath
[imagePullPolicy]: https://kubernetes.io/docs/concepts/containers/images/#updating-images
[imagePullSecrets]: https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-pod-that-uses-your-secret
[kafka output]: https://www.elastic.co/guide/en/beats/filebeat/7.10/kafka-output.html
[kubernetes secrets]: https://kubernetes.io/docs/concepts/configuration/secret/
[labels]: https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/
[nodeSelector]: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#nodeselector
[podSecurityContext]: https://kubernetes.io/docs/tasks/configure-pod-container/security-context/
[priorityClass]: https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#priorityclass
[probe]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/
[resources]: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/
[supported configurations]: https://github.com/elastic/helm-charts/tree/7.10/README.md#supported-configurations
[serviceAccount]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/
[tolerations]: https://kubernetes.io/docs/concepts/configuration/taint-and-toleration/
[updateStrategy]: https://kubernetes.io/docs/tasks/manage-daemon/update-daemon-set/#daemonset-update-strategy
[values.yaml]: https://github.com/elastic/helm-charts/tree/7.10/filebeat/values.yaml
