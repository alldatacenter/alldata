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
    - revisionName: K8S_MICROSERVICE|filemanage|_
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
            path: /sreworks/other/**
            servicePort: 80
            serviceName: "{{ Global.STAGE_ID }}-upload-filemanage"

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
          value: "sreworks_meta"
        - name: Global.SREWORKS_FILE_PREFIX
          value: "/gateway/minio/"
        - name: Global.UPLOAD_SUB_PATH
          value: "v1.0"
