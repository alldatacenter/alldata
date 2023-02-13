apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "prod"
    - name: ABM_CLUSTER
      value: "default-cluster"
    - name: CLOUD_TYPE
      value: "PaaS"
    - name: ENV_TYPE
      value: "PaaS" 
    - name: ENDPOINT_PAAS_PRODUCTOPS
      value: "${CORE_STAGE_ID}-${CORE_APP_ID}-paas-productops"
  components:
    - revisionName: K8S_MICROSERVICE|health|_
      scopes:
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"
        - scopeRef:
            apiVersion: apps.abm.io/v1
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
      traits:
        - name: service.trait.abm.io
          runtime: post
          spec:
            ports:
              - protocol: TCP
                port: 80
                targetPort: 7001
        - name: gateway.trait.abm.io
          runtime: post
          spec:
            path: /health/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-health-health'
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DATA_DB_HEALTH_NAME
          value: "sw_saas_health"
        - name: Global.DATA_DB_NAME
          value: "sw_saas_health" 
        - name: Global.DATA_DB_HOST
          value: "${DATAOPS_DB_HOST}"
        - name: Global.DATA_DB_PORT
          value: "${DATAOPS_DB_PORT}"
        - name: Global.DATA_DB_USER
          value: "${DATAOPS_DB_USER}"
        - name: Global.DATA_DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.KAFKA_ENDPOINT
          value: "${KAFKA_ENDPOINT}:9092"
 
