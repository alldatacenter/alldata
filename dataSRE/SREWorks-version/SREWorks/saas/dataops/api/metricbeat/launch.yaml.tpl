apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "${SAAS_STAGE_ID}"
    - name: ABM_CLUSTER
      value: "default-cluster"
    - name: CLOUD_TYPE
      value: "PaaS"
    - name: ENV_TYPE
      value: "PaaS"
    - name: APP_ID
      value: "dataops"
    - name: COMPONENT_NAME
      value: "metricbeat"
  components:
  - dataOutputs: []
    revisionName: "HELM|metricbeat|_"
    traits: []
    dataInputs: []
    scopes:
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Namespace"
        name: "${NAMESPACE_ID}"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Cluster"
        name: "master"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Stage"
        name: "${SAAS_STAGE_ID}"
    dependencies: []
    parameterValues:
    - name: "values"
      value:
        image: "${METRICBEAT_IMAGE}"
        imageTag: "${METRICBEAT_IMAGE_TAG}"
        daemonset:
          annotations:
            name: metricbeat
          labels: 
            k8s-app: metricbeat
          enabled: true
          extraEnvs:
            - name: NODE_NAME
              valueFrom:
                fieldRef:
                  fieldPath: spec.nodeName
            - name: NODE_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
          hostNetworking: true
          #dnsPolicy: ClusterFirstWithHostNet
          metricbeatConfig:
            metricbeat.yml: |
              metricbeat.modules:
              - module: kubernetes
                metricsets:
                  - container
                  - node
                  - pod
                  - system
                  - volume
                period: 1m
                host: "${NODE_NAME}"
                hosts: ["https://${NODE_IP}:10250"]
                bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
                ssl.verification_mode: "none"
                # If using Red Hat OpenShift remove ssl.verification_mode entry and
                # uncomment these settings:
                ssl.certificate_authorities:
                  - /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
                processors:
                - add_kubernetes_metadata: ~
              - module: kubernetes
                enabled: true
                metricsets:
                  - event
              - module: kubernetes
                metricsets:
                  - proxy
                period: 1m
                host: ${NODE_NAME}
                hosts: ["localhost:10249"]
              - module: system
                period: 1m
                metricsets:
                  - cpu
                  - load
                  - memory
                  - network
                  - process
                  - process_summary
                cpu.metrics: [percentages, normalized_percentages]
                processes: ['.*']
                process.include_top_n:
                  by_cpu: 5
                  by_memory: 5
              - module: system
                period: 1m
                metricsets:
                  - filesystem
                  - fsstat
                processors:
                - drop_event.when.regexp:
                    system.filesystem.mount_point: '^/(sys|cgroup|proc|dev|etc|host|lib)($|/)'

              metricbeat.autodiscover:
                providers:
                  - type: kubernetes
                    scope: cluster
                    node: ${NODE_NAME}
                    resource: service
                    templates:
                      - condition:
                          equals:
                            kubernetes.labels.sreworks-telemetry-metric: enable
                        config:
                          - module: http
                            metricsets:
                              - json
                            period: 1m
                            hosts: ["http://${data.host}:10080"]
                            namespace: "${data.kubernetes.namespace}#${data.kubernetes.service.name}"
                            path: "/"
                            method: "GET"

                  - type: kubernetes
                    scope: cluster
                    node: ${NODE_NAME}
                    unique: true
                    templates:
                      - config:
                          - module: kubernetes
                            hosts: ["kubecost-kube-state-metrics.sreworks-client.svc.cluster.local:8080"]
                            period: 1m
                            add_metadata: true
                            metricsets:
                              - state_node
                              - state_deployment
                              - state_daemonset
                              - state_replicaset
                              - state_pod
                              - state_container
                              - state_cronjob
                              - state_resourcequota
                              - state_statefulset
                              - state_service

              processors:
                - add_cloud_metadata:
              
              setup.ilm.enabled: auto
              setup.ilm.rollover_alias: "metricbeat"
              setup.ilm.pattern: "{now/d}-000001"
              setup.template.name: "metricbeat"
              setup.template.pattern: "metricbeat-*"

              output.elasticsearch:
                hosts: '${DATA_ES_HOST}:${DATA_ES_PORT}'
                index: "metricbeat-%{+yyyy.MM.dd}"
                username: "${DATA_ES_USER}"
                password: "${DATA_ES_PASSWORD}"

          resources:
            requests:
              cpu: "100m"
              memory: "100Mi"
            limits:
              cpu: "1000m"
              memory: "500Mi"
        deployment: 
          enabled: false

        kube_state_metrics:
          enabled: false

        clusterRoleRules:
        - apiGroups: [""]
          resources:
          - nodes
          - namespaces
          - events
          - pods
          verbs: ["get", "list", "watch"]
        - apiGroups: ["extensions"]
          resources:
          - replicasets
          verbs: ["get", "list", "watch"]
        - apiGroups: ["apps"]
          resources:
          - statefulsets
          - deployments
          - replicasets
          verbs: ["get", "list", "watch"]
        - apiGroups: [""]
          resources:
          - nodes/stats
          - nodes
          - services
          - endpoints
          - pods
          verbs: ["get", "list", "watch"]
        - nonResourceURLs:
            - "/metrics"
          verbs:
            - get
        - apiGroups:
            - coordination.k8s.io
          resources:
            - leases
          verbs:
            - '*'

        serviceAccount: "metricbeat-sa" 



      toFieldPaths:
      - "spec.values"




