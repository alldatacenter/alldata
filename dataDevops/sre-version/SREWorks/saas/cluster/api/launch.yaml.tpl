apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-cluster-package
  annotations:
    appId: cluster
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
  components:
    - revisionName: K8S_MICROSERVICE|clustermanage|_
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
            path: /sreworks/clustermanage/**
            servicePort: 80
            serviceName: '{{ Global.STAGE_ID }}-cluster-clustermanage'

      parameterValues:
        - name: REPLICAS
          value: 1
          toFieldPaths:
            - spec.replicas
        - name: Global.DB_NAME
          value: "sreworks_meta"
        - name: Global.APPMANAGER_ENDPOINT
          value: "http://${APPMANAGER_ENDPOINT}"
        - name: Global.AUTHPROXY_ENDPOINT
          value: "http://${AUTHPROXY_ENDPOINT}"
        - name: Global.APPMANAGER_USERNAME
          value: "${APPMANAGER_USERNAME}"
        - name: Global.APPMANAGER_PASSWORD
          value: "${APPMANAGER_PASSWORD}"
        - name: Global.APPMANAGER_CLIENT_ID
          value: "${APPMANAGER_CLIENT_ID}"
        - name: Global.APPMANAGER_CLIENT_SECRET
          value: "${APPMANAGER_CLIENT_SECRET}"

