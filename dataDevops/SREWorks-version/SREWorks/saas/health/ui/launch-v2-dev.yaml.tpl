apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-health-package
  annotations:
    appId: health
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
    - name: STAGE_ID
      value: 'dev'
      toFieldPaths:
        - spec.stageId
    traits:
    - name: gateway.trait.abm.io
      runtime: post
      spec:
        path: /health/**
        servicePort: 80
        serviceName: 'prod-health-health'
        routeId: "dev-health-health-master-${NAMESPACE_ID}-dev"
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
    - name: OVERWRITE_IS_DEVELOPMENT
      value: 'true'
      toFieldPaths:
        - spec.overwriteIsDevelopment
 

