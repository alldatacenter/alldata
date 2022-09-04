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
    - name: REDIS_HOST
      value: "{{ env.APPMANAGER_REDIS_HOST }}"
    - name: REDIS_PORT
      value: "{{ env.APPMANAGER_REDIS_PORT }}"
    - name: REDIS_PASSWORD
      value: "{{ env.APPMANAGER_REDIS_PASSWORD }}"
    - name: REDIS_DB
      value: "0"
    - name: NACOS_ENDPOINT
      value: "${CORE_STAGE_ID}-${CORE_APP_ID}-paas-nacos:8848"
    - name: NACOS_NAMESPACE
      value: "ad2d92c6-1a21-47ac-9da8-203fcbed9146" 
  components:
    - revisionName: K8S_MICROSERVICE|tkgone|_
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
            path: /v2/foundation/kg/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-search-tkgone'

      parameterValues:
        - name: KIND
          value: StatefulSet
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "search_saas_tkgone"
        #- name: Global.CONTAINER_IP_LIST_TKGONE
        #  value: "prod-sreworks-flycore-paas-tkgone-0.prod-sreworks-flycore-paas-tkgone,prod-sreworks-flycore-paas-tkgone-1.prod-sreworks-flycore-paas-tkgone"
        #- name: Global.CONTAINER_IP_LIST
        #  value: "prod-sreworks-flycore-paas-tkgone-0.prod-sreworks-flycore-paas-tkgone,prod-sreworks-flycore-paas-tkgone-1.prod-sreworks-flycore-paas-tkgone"
        - name: Global.ELASTICSEARCH_HOST
          value: "${DATA_ES_HOST}"
        - name: Global.ELASTICSEARCH_PORT
          value: "${DATA_ES_PORT}"
        - name: Global.ELASTICSEARCH_USER
          value: "${DATA_ES_USER}"
        - name: Global.ELASTICSEARCH_PASSWORD
          value: "${DATA_ES_PASSWORD}"
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
 


