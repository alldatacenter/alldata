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
  components:
    - revisionName: K8S_MICROSERVICE|plugin-aliyun-cluster|_
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
          labels:
            pluginType: CLUSTER
            accountType: aliyun
          ports:
          - name: main
            protocol: TCP
            port: 7001
            targetPort: 7001
          - name: manage
            protocol: TCP
            port: 7002
            targetPort: 7002
      parameterValues:
#    - revisionName: K8S_MICROSERVICE|plugin-aliyun-rds|_
#      scopes:
#        - scopeRef:
#            apiVersion: apps.abm.io/v1
#            kind: Cluster
#            name: "{{ Global.CLUSTER_ID }}"
#        - scopeRef:
#            apiVersion: apps.abm.io/v1
#            kind: Namespace
#            name: "{{ Global.NAMESPACE_ID }}"
#        - scopeRef:
#            apiVersion: apps.abm.io/v1
#            kind: Stage
#            name: "{{ Global.STAGE_ID }}"
#      traits:
#      - name: service.trait.abm.io
#        runtime: post
#        spec:
#          labels:
#            pluginType: RESOURCE
#            accountType: aliyun
#            resourceType: redis
#          ports:
#          - name: main
#            protocol: TCP
#            port: 7001
#            targetPort: 7001
#          - name: manage
#            protocol: TCP
#            port: 7002
#            targetPort: 7002
#      parameterValues:
    - revisionName: K8S_MICROSERVICE|plugin-account-aliyun|_
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
          labels:
            pluginType: ACCOUNT
            accountType: aliyun
          ports:
          - name: main
            protocol: TCP
            port: 7001
            targetPort: 7001
          - name: manage
            protocol: TCP
            port: 7002
            targetPort: 7002
      parameterValues:
    - revisionName: K8S_JOB|resource-upload|_
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
      parameterValues: []
 

