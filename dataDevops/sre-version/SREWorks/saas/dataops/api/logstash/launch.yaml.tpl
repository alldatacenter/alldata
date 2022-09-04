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
  components:
  - dataOutputs: []
    revisionName: "HELM|logstash|_"
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
        #image: "sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/logstash"
        #imageTag: "7.10.2"
        image: "${LOGSTASH_IMAGE}"
        imageTag: "${LOGSTASH_IMAGE_TAG}"
        logstashConfig:
          logstash.yml: |
            http.host: 0.0.0.0
            xpack.monitoring.enabled: true
            xpack.monitoring.elasticsearch.username: '${DATA_ES_USER}'
            xpack.monitoring.elasticsearch.password: '${DATA_ES_PASSWORD}'
            xpack.monitoring.elasticsearch.hosts: ["${DATA_ES_HOST}:${DATA_ES_PORT}"]
        logstashPipeline:
          logstash.conf: |
            input {
              elasticsearch {
                hosts => "${DATA_ES_HOST}:${DATA_ES_PORT}"
                user => "${DATA_ES_USER}"
                password => "${DATA_ES_PASSWORD}"
                index => "metricbeat"
                query => '{"query":{"bool":{"must":[{"range":{"@timestamp":{"gte":"now-1m/m","lt":"now/m"}}},{"query_string":{"query":"kubernetes.labels.sreworks-telemetry-metric:enable AND metricset.name:json"}},{"exists":{"field":"http"}}]}},"sort":["service.address"]}'
                schedule => "* * * * *"
                scroll => "5m"
                size => 10000
              }
            }
            output {
              kafka {
                bootstrap_servers => "sreworks-kafka.sreworks.svc.cluster.local:9092"
                codec => json
                topic_id => "sreworks-telemetry-metric"
              }
            }

        volumeClaimTemplate:  
          accessModes: [ "ReadWriteOnce" ]
          storageClassName: "${GLOBAL_STORAGE_CLASS}"
          resources:
            requests:
              storage: 50Gi
        
        service:
          type: ClusterIP
          loadBalancerIP: ""
          ports:
            - name: beats
              port: 5044
              protocol: TCP
              targetPort: 5044
            - name: http
              port: 8080
              protocol: TCP
              targetPort: 8080

      toFieldPaths:
      - "spec.values"
