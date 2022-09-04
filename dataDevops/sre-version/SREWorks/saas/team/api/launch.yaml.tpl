apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-team-package
  annotations:
    appId: team
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
    - revisionName: K8S_MICROSERVICE|team|_
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
            path: "/sreworks/teammanage/**"
            servicePort: 80
            serviceName: "prod-team-team"

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
        - name: Global.TEAM_DEFAULT_REPO
          # {"name":"default","url":"https://code.aliyun.com/sreworks","ciToken":"","ciAccount":""}
          value: eyJuYW1lIjoiZGVmYXVsdCIsInVybCI6Imh0dHBzOi8vY29kZS5hbGl5dW4uY29tL3NyZXdvcmtzIiwiY2lUb2tlbiI6IiIsImNpQWNjb3VudCI6IiJ9
