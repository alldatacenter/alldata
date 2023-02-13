apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-app-package
  annotations:
    appId: app
    clusterId: master
    namespaceId: ${NAMESPACE_ID}
    stageId: dev
spec:
  components:
  - dataInputs: []
    dataOutputs: []
    dependencies: []
    revisionName: INTERNAL_ADDON|productopsv2|_
    scopes:
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Namespace
        name: ${NAMESPACE_ID}
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Stage
        name: 'dev'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: TARGET_ENDPOINT
      value: "${CORE_STAGE_ID}-${CORE_APP_ID}-paas-action"
      toFieldPaths:
        - spec.targetEndpoint
    - name: STAGE_ID
      value: 'dev'
      toFieldPaths:
        - spec.stageId
    traits:
    - name: gateway.trait.abm.io
      runtime: post
      spec:
        path: "/sreworks/**"
        servicePort: 80
        serviceName: "prod-app-app"
        routeId: "dev-app-app-master-${NAMESPACE_ID}-dev"
        order: 5000

  - dataInputs: []
    dataOutputs: []
    dependencies: []
    revisionName: INTERNAL_ADDON|appmeta|_
    scopes:
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Namespace
        name: ${NAMESPACE_ID}
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Stage
        name: 'dev'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: STAGE_ID
      value: 'dev'
      toFieldPaths:
        - spec.stageId
