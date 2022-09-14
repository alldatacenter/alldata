apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-app-package
  annotations:
    appId: app
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
    - revisionName: K8S_MICROSERVICE|app|_
      dependencies:
        - component: RESOURCE_ADDON|system-env@system-env
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
            path: "/sreworks/**"
            servicePort: 80
            serviceName: "prod-app-app"
            order: 5000
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
    - dataInputs: []
      dataOutputs:
        - fieldPath: '{{ spec.env.DB_HOST }}'
          name: Global.DB_HOST
        - fieldPath: '{{ spec.env.DB_PASSWORD }}'
          name: Global.DB_PASSWORD
        - fieldPath: '{{ spec.env.DB_PORT }}'
          name: Global.DB_PORT
        - fieldPath: '{{ spec.env.DB_USER }}'
          name: Global.DB_USER
      dependencies: []
      parameterValues:
        - name: keys
          toFieldPaths:
            - spec.keys
          value:
            - DB_HOST
            - DB_PASSWORD
            - DB_PORT
            - DB_USER
      revisionName: RESOURCE_ADDON|system-env@system-env|1.0
      scopes:
        - scopeRef:
            apiVersion: core.oam.dev/v1alpha2
            kind: Stage
            name: "{{ Global.STAGE_ID }}"
        - scopeRef:
            apiVersion: core.oam.dev/v1alpha2
            kind: Cluster
            name: "{{ Global.CLUSTER_ID }}" 
        - scopeRef:
            apiVersion: core.oam.dev/v1alpha2
            kind: Namespace
            name: "{{ Global.NAMESPACE_ID }}"

