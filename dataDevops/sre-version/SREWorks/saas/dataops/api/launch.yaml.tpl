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
    - name: APP_ID
      value: "dataops" 
    - name: DB_HOST
      value: "{{ env.APPMANAGER_DB_HOST }}"
    - name: DB_PORT
      value: "{{ env.APPMANAGER_DB_PORT }}"
    - name: DB_USER
      value: "{{ env.APPMANAGER_DB_USER }}"
    - name: DB_PASSWORD
      value: "{{ env.APPMANAGER_DB_PASSWORD }}"
  components:
    - revisionName: K8S_MICROSERVICE|dataset|_
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
            path: /dataset/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-dataops-dataset.{{ Global.NAMESPACE_ID }}'

      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DATA_DB_DATASET_NAME
          value: "sw_saas_dataset"
        - name: Global.DATA_DB_DATASOURCE_NAME
          value: "sw_saas_datasource"
        - name: Global.DATA_DB_HOST
          value: "${DATAOPS_DB_HOST}"
        - name: Global.DATA_DB_PORT
          value: "${DATAOPS_DB_PORT}"
        - name: Global.DATA_DB_USER
          value: "${DATAOPS_DB_USER}"
        - name: Global.DATA_DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.DATA_SKYW_HOST
          value: "{{ Global.STAGE_ID }}-{{ Global.APP_ID }}-skywalking-oap" 
        - name: Global.DATA_SKYW_PORT
          value: "11800" 
        - name: Global.DATA_SKYW_ENABLE
          value: "true" 
        - name: Global.DATA_DB_PMDB_NAME
          value: "pmdb" 
 

    - revisionName: K8S_JOB|dataset-postrun|_
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
      traits: []
      parameterValues:
        - name: Global.MINIO_ENDPOINT
          value: "${ENDPOINT_PAAS_MINIO}"
        - name: Global.MINIO_ACCESS_KEY
          value: "${MINIO_ACCESS_KEY}"
        - name: Global.MINIO_SECRET_KEY
          value: "${MINIO_SECRET_KEY}"
        - name: Global.DATA_ES_USER
          value: "${DATA_ES_USER}"
        - name: Global.DATA_ES_PASSWORD
          value: "${DATA_ES_PASSWORD}"


    - revisionName: K8S_MICROSERVICE|pmdb|_
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
            path: /pmdb/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-dataops-pmdb.{{ Global.NAMESPACE_ID }}'

      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DATA_DB_PMDB_NAME
          value: "pmdb"
        - name: Global.DATA_DB_HOST
          value: "${DATAOPS_DB_HOST}"
        - name: Global.DATA_DB_PORT
          value: "${DATAOPS_DB_PORT}"
        - name: Global.DATA_DB_USER
          value: "${DATAOPS_DB_USER}"
        - name: Global.DATA_DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.DATA_SKYW_HOST
          value: "{{ Global.STAGE_ID }}-{{ Global.APP_ID }}-skywalking-oap" 
        - name: Global.DATA_SKYW_PORT
          value: "11800" 
        - name: Global.DATA_SKYW_ENABLE
          value: "true"
        - name: Global.KAFKA_ENDPOINT
          value: "${KAFKA_ENDPOINT}:9092"
        - name: Global.DATA_ES_HOST
          value: "${DATA_ES_HOST}"
        - name: Global.DATA_ES_PORT
          value: "${DATA_ES_PORT}"
        - name: Global.DATA_ES_USER
          value: "${DATA_ES_USER}"
        - name: Global.DATA_ES_PASSWORD
          value: "${DATA_ES_PASSWORD}"
 

    - revisionName: K8S_MICROSERVICE|warehouse|_
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
            path: /warehouse/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-dataops-warehouse.{{ Global.NAMESPACE_ID }}'

      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DATA_DB_WAREHOUSE_NAME
          value: "sw_saas_warehouse"
        - name: Global.DATA_DB_HOST
          value: "${DATAOPS_DB_HOST}"
        - name: Global.DATA_DB_PORT
          value: "${DATAOPS_DB_PORT}"
        - name: Global.DATA_DB_USER
          value: "${DATAOPS_DB_USER}"
        - name: Global.DATA_DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.DATA_ES_HOST
          value: "${DATA_ES_HOST}"
        - name: Global.DATA_ES_PORT
          value: "${DATA_ES_PORT}"
        - name: Global.DATA_ES_USER
          value: "${DATA_ES_USER}"
        - name: Global.DATA_ES_PASSWORD
          value: "${DATA_ES_PASSWORD}"
 

    - revisionName: K8S_JOB|metric-flink-init|_
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
      traits: []
      parameterValues:
        - name: Global.DATA_DB_PMDB_NAME
          value: "pmdb"
        - name: Global.DATA_DB_HEALTH_NAME
          value: sw_saas_health 
        - name: Global.HEALTH_ENDPOINT
          value: "{{ Global.STAGE_ID }}-health-health.sreworks.svc.cluster.local:80"
        - name: Global.DATA_DB_HOST
          value: "${DATAOPS_DB_HOST}"
        - name: Global.DATA_DB_PORT
          value: "${DATAOPS_DB_PORT}"
        - name: Global.DATA_DB_USER
          value: "${DATAOPS_DB_USER}"
        - name: Global.DATA_DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.MINIO_ENDPOINT
          value: "${ENDPOINT_PAAS_MINIO}"
        - name: Global.MINIO_ACCESS_KEY
          value: "${MINIO_ACCESS_KEY}"
        - name: Global.MINIO_SECRET_KEY
          value: "${MINIO_SECRET_KEY}"
        - name: Global.VVP_ENDPOINT
          value: "{{ Global.STAGE_ID }}-{{ Global.APP_ID }}-ververica-platform-ververica-platform"
        - name: Global.KAFKA_URL
          value: "${KAFKA_ENDPOINT}:9092" 
        - name: Global.ES_URL
          value: "{{ Global.STAGE_ID }}-{{ Global.APP_ID }}-elasticsearch-master:9200"
        - name: Global.DATA_ES_HOST
          value: "${DATA_ES_HOST}"
        - name: Global.DATA_ES_PORT
          value: "${DATA_ES_PORT}"
        - name: Global.DATA_ES_USER
          value: "${DATA_ES_USER}"
        - name: Global.DATA_ES_PASSWORD
          value: "${DATA_ES_PASSWORD}"
 
