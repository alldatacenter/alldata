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
      value: "aiops" 
  components:
    - revisionName: K8S_MICROSERVICE|aisp|_
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
        - 
          name: gateway.trait.abm.io
          runtime: post
          spec:
            path: /aiops/aisp/**
            serviceName: '{{ Global.STAGE_ID }}-aiops-aisp.{{ Global.NAMESPACE_ID }}'
      parameterValues:
        - name: Global.DB_PORT
          value: '3306'
        - name: Global.DB_USER
          value: '${DATAOPS_DB_USER}'
        - name: Global.DB_PASSWORD
          value: "${DATAOPS_DB_PASSWORD}"
        - name: Global.DB_NAME
          value: aiops_aisp
        - name: Global.CACHE_TYPE
          value: local
        - name: Global.DB_HOST
          value: '${DATAOPS_DB_HOST}'
        - name: Global.ACCOUNT_SUPER_CLIENT_ID
          value: "${ACCOUNT_SUPER_CLIENT_ID}"
        - name: Global.ACCOUNT_SUPER_CLIENT_SECRET
          value: "${ACCOUNT_SUPER_CLIENT_SECRET}"
        - name: Global.ACCOUNT_SUPER_ACCESS_ID
          value: "admin"
        - name: Global.ACCOUNT_SUPER_SECRET_KEY
          value: "test-super-secret-key"
        - name: Global.ACCOUNT_SUPER_ACCESS_KEY
          value: "test-super-secret-key"


    #- revisionName: K8S_MICROSERVICE|drilldown|_
    #  scopes:
    #    - scopeRef:
    #        apiVersion: apps.abm.io/v1
    #        kind: Cluster
    #        name: "{{ Global.CLUSTER_ID }}"
    #    - scopeRef:
    #        apiVersion: apps.abm.io/v1
    #        kind: Namespace
    #        name: "{{ Global.NAMESPACE_ID }}"
    #    - scopeRef:
    #        apiVersion: apps.abm.io/v1
    #        kind: Stage
    #        name: "{{ Global.STAGE_ID }}"
    #  traits:
    #    - name: service.trait.abm.io
    #      runtime: post
    #      spec:
    #        ports:
    #          - protocol: TCP
    #            port: 80
    #            targetPort: 5000
    #    - 
    #      name: gateway.trait.abm.io
    #      runtime: post
    #      spec:
    #        path: /aiops/drilldown/**
    #        serviceName: '{{ Global.STAGE_ID }}-aiops-drilldown.{{ Global.NAMESPACE_ID }}'
    #  parameterValues: []

    - revisionName: K8S_MICROSERVICE|anomalydetection|_
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
                targetPort: 5000
        - 
          name: gateway.trait.abm.io
          runtime: post
          spec:
            path: /aiops/anomalydetection/**
            serviceName: '{{ Global.STAGE_ID }}-aiops-anomalydetection.{{ Global.NAMESPACE_ID }}'
      parameterValues: []

    - revisionName: K8S_MICROSERVICE|processstrategy|_
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
                targetPort: 5000
        - 
          name: gateway.trait.abm.io
          runtime: post
          spec:
            path: /aiops/processstrategy/**
            serviceName: '{{ Global.STAGE_ID }}-aiops-processstrategy.{{ Global.NAMESPACE_ID }}'
      parameterValues:
        - name: Global.TASKPLATFORM_SUBMIT_URL
          value: 'http://prod-job-job-master.sreworks/job/start'
        - name: Global.TASKPLATFORM_QUERY_URL
          value: 'http://prod-job-job-master.sreworks/jobInstance/get'
        - name: Global.AISP_URL
          value: 'http://prod-aiops-aisp' 







