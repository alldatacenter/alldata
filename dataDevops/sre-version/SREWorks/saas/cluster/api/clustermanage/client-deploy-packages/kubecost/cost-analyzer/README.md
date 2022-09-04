# Kubecost helm chart
Helm chart for the Kubecost project, which is created to monitor and manage Kubernetes resource spend. Please contact team@kubecost.com or visit [kubecost.com](http://kubecost.com) for more info.

While Helm is the [recommended install path](http://kubecost.com/install), these resources can also be deployed with the following command:<a name="manifest"></a>

`kubectl apply -f https://raw.githubusercontent.com/kubecost/cost-analyzer-helm-chart/master/kubecost.yaml --namespace kubecost`

<a name="config-options"></a><br/>
The following table lists the commonly used configurable parameters of the Kubecost Helm chart and their default values.

Parameter | Description | Default
--------- | ----------- | -------
`global.prometheus.enabled` | If false, use an existing Prometheus install. [More info](http://docs.kubecost.com/custom-prom). | `true`
`prometheus.kube-state-metrics.disabled` | If false, deploy [kube-state-metrics](https://github.com/kubernetes/kube-state-metrics) for Kubernetes metrics | `false`
`prometheus.kube-state-metrics.resources` | Set kube-state-metrics resource requests and limits. | `{}`
`prometheus.server.persistentVolume.enabled` | If true, Prometheus server will create a Persistent Volume Claim. | `true`
`prometheus.server.persistentVolume.size` | Prometheus server data Persistent Volume size. Default set to retain ~6000 samples per second for 15 days. | `32Gi`
`prometheus.server.retention` | Determines when to remove old data. | `15d`
`prometheus.server.resources` | Prometheus server resource requests and limits. | `{}`
`prometheus.nodeExporter.resources` | Node exporter resource requests and limits. | `{}`
`prometheus.nodeExporter.enabled` `prometheus.serviceAccounts.nodeExporter.create` | If false, do not crate NodeExporter daemonset.  | `true`
`prometheus.alertmanager.persistentVolume.enabled` | If true, Alertmanager will create a Persistent Volume Claim. | `true`
`prometheus.pushgateway.persistentVolume.enabled` | If true, Prometheus Pushgateway will create a Persistent Volume Claim. | `true`
`persistentVolume.enabled` | If true, Kubecost will create a Persistent Volume Claim for product config data.  | `true`
`persistentVolume.size` | Define PVC size for cost-analyzer  | `0.2Gi`
`persistentVolume.dbSize` | Define PVC size for cost-analyzer's flat file database  | `32.0Gi`
`ingress.enabled` | If true, Ingress will be created | `false`
`ingress.annotations` | Ingress annotations | `{}`
`ingress.paths` | Ingress paths | `["/"]`
`ingress.hosts` | Ingress hostnames | `[cost-analyzer.local]`
`ingress.tls` | Ingress TLS configuration (YAML) | `[]`
`networkPolicy.enabled` | If true, create a NetworkPolicy to deny egress  | `false`
`networkPolicy.costAnalyzer.enabled` | If true, create a newtork policy for cost-analzyer | `false`
`networkPolicy.costAnalyzer.annotations` | Annotations to be added to the network policy | `{}`
`networkPolicy.costAnalyzer.additionalLabels` | Additional labels to be added to the network policy | `{}`
`networkPolicy.costAnalyzer.ingressRules` | A list of network policy ingress rules | `null`
`networkPolicy.costAnalyzer.egressRules` | A list of network policy egress rules | `null`
`networkCosts.enabled` | If true, collect network allocation metrics [More info](http://docs.kubecost.com/network-allocation) | `false`
`networkCosts.podMonitor.enabled` | If true, a [PodMonitor](https://github.com/coreos/prometheus-operator/blob/master/Documentation/api.md#podmonitor) for the network-cost daemonset is created | `false`
`serviceMonitor.enabled` | Set this to `true` to create ServiceMonitor for Prometheus operator | `false`
`serviceMonitor.additionalLabels` | Additional labels that can be used so ServiceMonitor will be discovered by Prometheus | `{}`
`prometheusRule.enabled` | Set this to `true` to create PrometheusRule for Prometheus operator | `false`
`prometheusRule.additionalLabels` | Additional labels that can be used so PrometheusRule will be discovered by Prometheus | `{}`
`grafana.resources` | Grafana resource requests and limits. | `{}`
`grafana.sidecar.datasources.defaultDatasourceEnabled` | Set this to `false` to disable creation of Prometheus datasource in Grafana | `true`
`serviceAccount.create` | Set this to `false` if you want to create the service account `kubecost-cost-analyzer` on your own | `true`
`tolerations` | node taints to tolerate | `[]`
`affinity` | pod affinity | `{}`
