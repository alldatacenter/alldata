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
      value: "filebeat"
  components:
  - dataOutputs: []
    revisionName: "HELM|filebeat|_"
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
        image: "${FILEBEAT_IMAGE}"
        imageTag: "${FILEBEAT_IMAGE_TAG}"
        podAnnotations:
          name: filebeat
        labels: 
          k8s-app: filebeat
        extraEnvs:
          - name: NODE_NAME
            valueFrom:
              fieldRef:
                fieldPath: spec.nodeName
      
        hostNetworking: true
        #dnsPolicy: ClusterFirstWithHostNet
        filebeatConfig:
          filebeat.yml: |
            filebeat.autodiscover:
              providers:
                - type: kubernetes
                  node: ${NODE_NAME}
                  resource: pod
                  scope: node
                  templates:
                    - condition:
                        equals:
                          kubernetes.labels.sreworks-telemetry-log: enable
                      config:
                        - type: container
                          paths:
                            - /var/log/containers/*${data.kubernetes.container.id}.log
                          multiline:
                            type: pattern
                            pattern: '^(\[)?20\d{2}-(1[0-2]|0?[1-9])-(0?[1-9]|[1-2]\d|30|31)'
                            negate: true
                            match: after
                          processors:
                            - add_kubernetes_metadata:
                                host: ${NODE_NAME}
                                matchers:
                                - logs_path:
                                    logs_path: "/var/log/containers/"

            setup.ilm.enabled: auto
            setup.ilm.rollover_alias: "filebeat"
            setup.ilm.pattern: "{now/d}-000001"
            setup.template.name: "filebeat"
            setup.template.pattern: "filebeat-*"
            
            output.elasticsearch:
              hosts: '${DATA_ES_HOST}:${DATA_ES_PORT}'
              index: "filebeat-%{+yyyy.MM.dd}"
              username: "${DATA_ES_USER}"
              password: "${DATA_ES_PASSWORD}"

      toFieldPaths:
      - "spec.values"
