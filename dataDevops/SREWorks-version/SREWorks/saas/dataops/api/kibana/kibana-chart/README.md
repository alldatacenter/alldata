# Kibana Helm Chart

This Helm chart is a lightweight way to configure and run our official
[Kibana Docker image][].

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
  - [Deprecated](#deprecated)
- [FAQ](#faq)
  - [How to deploy this chart on a specific K8S distribution?](#how-to-deploy-this-chart-on-a-specific-k8s-distribution)
  - [How to use Kibana with security (authentication and TLS) enabled?](#how-to-use-kibana-with-security-authentication-and-tls-enabled)
  - [How to install OSS version of Kibana?](#how-to-install-oss-version-of-kibana)
  - [How to install plugins?](#how-to-install-plugins)
  - [How to import objects post-deployment?](#how-to-import-objects-post-deployment)
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
  - with Helm 3: `helm install kibana --version <version> elastic/kibana`
  - with Helm 2 (deprecated): `helm install --name kibana --version <version> elastic/kibana`
### Install development version from a branch

* Clone the git repo: `git clone git@github.com:elastic/helm-charts.git`

* Checkout the branch : `git checkout 7.10`

* Install it:
  - with Helm 3: `helm install kibana ./helm-charts/kibana --set imageTag=7.10.2`
  - with Helm 2 (deprecated): `helm install --name kibana ./helm-charts/kibana --set imageTag=7.10.2`


## Upgrading

Please always check [CHANGELOG.md][] and [BREAKING_CHANGES.md][] before
upgrading to a new chart version.


## Usage notes

* Automated testing of this chart is currently only run against GKE (Google
Kubernetes Engine).

* This repo includes a number of [examples][] configurations which can be used
as a reference. They are also used in the automated testing of this chart.


## Configuration

| Parameter             | Description                                                                                                                                                                                    | Default                            |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| `affinity`            | Configurable [affinity][]                                                                                                                                                                      | `{}`                               |
| `elasticsearchHosts`  | The URLs used to connect to Elasticsearch                                                                                                                                                      | `http://elasticsearch-master:9200` |
| `envFrom`             | Templatable string to be passed to the [environment from variables][] which will be appended to the `envFrom:` definition for the container                                                    | `[]`                               |
| `extraContainers`     | Templatable string of additional containers to be passed to the `tpl` function                                                                                                                 | `""`                               |
| `extraEnvs`           | Extra [environment variables][] which will be appended to the `env:` definition for the container                                                                                              | see [values.yaml][]                |
| `extraInitContainers` | Templatable string of additional containers to be passed to the `tpl` function                                                                                                                 | `""`                               |
| `fullnameOverride`    | Overrides the full name of the resources. If not set the name will default to " `.Release.Name` - `.Values.nameOverride orChart.Name` "                                                        | `""`                               |
| `healthCheckPath`     | The path used for the readinessProbe to check that Kibana is ready. If you are setting `server.basePath` you will also need to update this to `/${basePath}/app/kibana`                        | `/app/kibana`                      |
| `hostAliases`         | Configurable [hostAliases][]                                                                                                                                                                   | `[]`                               |
| `httpPort`            | The http port that Kubernetes will use for the healthchecks and the service                                                                                                                    | `5601`                             |
| `imagePullPolicy`     | The Kubernetes [imagePullPolicy][]value                                                                                                                                                        | `IfNotPresent`                     |
| `imagePullSecrets`    | Configuration for [imagePullSecrets][] so that you can use a private registry for your image                                                                                                   | `[]`                               |
| `imageTag`            | The Kibana Docker image tag                                                                                                                                                                    | `7.10.2`                  |
| `image`               | The Kibana Docker image                                                                                                                                                                        | `docker.elastic.co/kibana/kibana`  |
| `ingress`             | Configurable [ingress][] to expose the Kibana service.                                                                                                                                         | see [values.yaml][]                |
| `kibanaConfig`        | Allows you to add any config files in `/usr/share/kibana/config/` such as `kibana.yml` See [values.yaml][] for an example of the formatting                                                    | `{}`                               |
| `labels`              | Configurable [labels][] applied to all Kibana pods                                                                                                                                             | `{}`                               |
| `lifecycle`           | Allows you to add [lifecycle hooks][]. See [values.yaml][] for an example of the formatting                                                                                                    | `{}`                               |
| `nameOverride`        | Overrides the chart name for resources. If not set the name will default to `.Chart.Name`                                                                                                      | `""`                               |
| `nodeSelector`        | Configurable [nodeSelector][] so that you can target specific nodes for your Kibana instances                                                                                                  | `{}`                               |
| `podAnnotations`      | Configurable [annotations][] applied to all Kibana pods                                                                                                                                        | `{}`                               |
| `podSecurityContext`  | Allows you to set the [securityContext][] for the pod                                                                                                                                          | see [values.yaml][]                |
| `priorityClassName`   | The name of the [PriorityClass][]. No default is supplied as the PriorityClass must be created first                                                                                           | `""`                               |
| `protocol`            | The protocol that will be used for the readinessProbe. Change this to `https` if you have `server.ssl.enabled: true` set                                                                       | `http`                             |
| `readinessProbe`      | Configuration for the readiness [probe][]                                                                                                                                                      | see [values.yaml][]                |
| `replicas`            | Kubernetes replica count for the Deployment (i.e. how many pods)                                                                                                                               | `1`                                |
| `resources`           | Allows you to set the [resources][] for the Deployment                                                                                                                                         | see [values.yaml][]                |
| `secretMounts`        | Allows you easily mount a secret as a file inside the Deployment. Useful for mounting certificates and other secrets. See [values.yaml][] for an example                                       | `[]`                               |
| `securityContext`     | Allows you to set the [securityContext][] for the container                                                                                                                                    | see [values.yaml][]                |
| `serverHost`          | The [server.host][] Kibana setting. This is set explicitly so that the default always matches what comes with the Docker image                                                                 | `0.0.0.0`                          |
| `serviceAccount`      | Allows you to overwrite the "default" [serviceAccount][] for the pod                                                                                                                           | `[]`                               |
| `service`             | Configurable [service][] to expose the Kibana service.                                                                                                                                         | see [values.yaml][]                |
| `tolerations`         | Configurable [tolerations][])                                                                                                                                                                  | `[]`                               |
| `updateStrategy`      | Allows you to change the default [updateStrategy][] for the Deployment. A [standard upgrade][] of Kibana requires a full stop and start which is why the default strategy is set to `Recreate` | `type: Recreate`                   |

### Deprecated

| Parameter          | Description                                                                          | Default |
|--------------------|--------------------------------------------------------------------------------------|---------|
| `elasticsearchURL` | The URL used to connect to Elasticsearch. needs to be used for Kibana versions < 6.6 | `""`    |


## FAQ

### How to deploy this chart on a specific K8S distribution?

This chart is highly tested with [GKE][], but some K8S distribution also
requires specific configurations.

We provide examples of configuration for the following K8S providers:

- [OpenShift][]

### How to use Kibana with security (authentication and TLS) enabled?

This Helm chart can use existing [Kubernetes secrets][] to setup
credentials or certificates for examples. These secrets should be created
outside of this chart and accessed using [environment variables][] and volumes.

An example can be found in [examples/security][].

### How to install OSS version of Kibana?

Deploying OSS version of Elasticsearch can be done by setting `image` value to
[kibana OSS Docker image][]

An example of Kibana deployment using OSS version can be found in
[examples/oss][].

### How to install plugins?

The recommended way to install plugins into our Docker images is to create a
custom Docker image.

The Dockerfile would look something like:

```
ARG kibana_version
FROM docker.elastic.co/kibana/kibana:${kibana_version}

RUN bin/kibana-plugin install <plugin_url>
```

And then updating the `image` in values to point to your custom image.

There are a couple reasons we recommend this:

1. Tying the availability of Kibana to the download service to install plugins
is not a great idea or something that we recommend. Especially in Kubernetes
where it is normal and expected for a container to be moved to another host at
random times.
2. Mutating the state of a running Docker image (by installing plugins) goes
against best practices of containers and immutable infrastructure.

### How to import objects post-deployment?

You can use `postStart` [lifecycle hooks][] to run code triggered after a
container is created.

Here is an example of `postStart` hook to import an index-pattern and a
dashboard:

```yaml
lifecycle:
  postStart:
    exec:
      command:
        - bash
        - -c
        - |
          #!/bin/bash
          # Import a dashboard
          KB_URL=http://localhost:5601
          while [[ "$(curl -s -o /dev/null -w '%{http_code}\n' -L $KB_URL)" != "200" ]]; do sleep 1; done
          curl -XPOST "$KB_URL/api/kibana/dashboards/import" -H "Content-Type: application/json" -H 'kbn-xsrf: true' -d'"objects":[{"type":"index-pattern","id":"my-pattern","attributes":{"title":"my-pattern-*"}},{"type":"dashboard","id":"my-dashboard","attributes":{"title":"Look at my dashboard"}}]}'
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
[default elasticsearch helm chart]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/README.md#default
[environment variables]: https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/#using-environment-variables-inside-of-your-config
[environment from variables]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#configure-all-key-value-pairs-in-a-configmap-as-container-environment-variables
[examples]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples
[examples/oss]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples/oss
[examples/security]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples/security
[gke]: https://cloud.google.com/kubernetes-engine
[helm]: https://helm.sh
[hostAliases]: https://kubernetes.io/docs/concepts/services-networking/add-entries-to-pod-etc-hosts-with-host-aliases/
[imagePullPolicy]: https://kubernetes.io/docs/concepts/containers/images/#updating-images
[imagePullSecrets]: https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-pod-that-uses-your-secret
[ingress]: https://kubernetes.io/docs/concepts/services-networking/ingress/
[kibana docker image]: https://www.elastic.co/guide/en/kibana/7.10/docker.html
[kibana oss docker image]: https://www.docker.elastic.co/r/kibana/kibana-oss
[kubernetes secrets]: https://kubernetes.io/docs/concepts/configuration/secret/
[labels]: https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/
[lifecycle hooks]: https://kubernetes.io/docs/concepts/containers/container-lifecycle-hooks/
[nodeSelector]: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#nodeselector
[openshift]: https://github.com/elastic/helm-charts/tree/7.10/kibana/examples/openshift
[priorityClass]: https://kubernetes.io/docs/concepts/configuration/pod-priority-preemption/#priorityclass
[probe]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/
[resources]: https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/
[security enabled elasticsearch cluster]: https://github.com/elastic/helm-charts/tree/7.10/elasticsearch/README.md#security
[securityContext]: https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod
[server.host]: https://www.elastic.co/guide/en/kibana/7.10/settings.html
[service]: https://kubernetes.io/docs/concepts/services-networking/service/
[serviceAccount]: https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/
[standard upgrade]: https://www.elastic.co/guide/en/kibana/7.10/upgrade-standard.html
[supported configurations]: https://github.com/elastic/helm-charts/tree/7.10/README.md#supported-configurations
[tolerations]: https://kubernetes.io/docs/concepts/configuration/taint-and-toleration/
[updateStrategy]: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment
[values.yaml]: https://github.com/elastic/helm-charts/tree/7.10/kibana/values.yaml
