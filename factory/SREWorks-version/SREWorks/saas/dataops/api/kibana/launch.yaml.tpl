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
      value: "kibana"
  components:
  - dataOutputs: []
    revisionName: "HELM|kibana|_"
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
        elasticsearchHosts: "http://${DATA_ES_HOST}:${DATA_ES_PORT}"

        kibanaConfig:
           kibana.yml: |-
             elasticsearch.username: ${DATA_ES_USER}
             elasticsearch.password: ${DATA_ES_PASSWORD}
           #  server.defaultRoute: /gateway/dataops-kibana

        ingress:
          enabled: false

        image: "${KIBANA_IMAGE}"
        imageTag: "${KIBANA_IMAGE_TAG}"

        resources:
          requests:
            cpu: "200m"
            memory: 512Mi
          limits:
            cpu: "300m"
            memory: 512Mi

      toFieldPaths:
      - "spec.values"




