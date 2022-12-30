apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-job-package
  annotations:
    appId: job
    clusterId: master
    namespaceId: ${NAMESPACE_ID} 
    stageId: prod
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "prod"
    - name: REDIS_HOST
      value: "{{ env.APPMANAGER_REDIS_HOST }}"
    - name: REDIS_PORT
      value: "{{ env.APPMANAGER_REDIS_PORT }}"
    - name: REDIS_PASSWORD
      value: "{{ env.APPMANAGER_REDIS_PASSWORD }}"
    - name: REDIS_DB
      value: "0"
    - name: ES_ENDPOINT
      value: "http://${DATA_ES_HOST}:${DATA_ES_PORT}"
    - name: ES_USERNAME
      value: "${DATA_ES_USER}"
    - name: ES_PASSWORD
      value: "${DATA_ES_PASSWORD}"
  components:
    - revisionName: K8S_MICROSERVICE|job-master|_
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
                targetPort: 17001
        - name: gateway.trait.abm.io
          runtime: post
          spec:
            path: "/sreworks-job/**"
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-job-job-master'

        - name: timezoneSync.trait.abm.io
          runtime: pre
          spec:
            timezone: Asia/Shanghai

      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "sreworks_saas_job"


    - revisionName: K8S_MICROSERVICE|job-worker|_
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
        - name: timezoneSync.trait.abm.io
          runtime: pre
          spec:
            timezone: Asia/Shanghai
      parameterValues:
        - name: KIND
          value: Deployment
          toFieldPaths:
            - spec.kind
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.SREWORKS_JOB_MASTER_ENDPOINT
          value: "http://prod-job-job-master"

