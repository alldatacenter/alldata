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
        name: 'prod'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: STAGE_ID
      value: 'prod'
      toFieldPaths:
        - spec.stageId
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
        name: 'prod'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: STAGE_ID
      value: 'prod'
      toFieldPaths:
        - spec.stageId
    - name: OVERWRITE_IS_DEVELOPMENT
      value: 'true'
      toFieldPaths:
        - spec.overwriteIsDevelopment
  - dataInputs: []
    dataOutputs: []
    dependencies: []
    revisionName: INTERNAL_ADDON|developmentmeta|_
    scopes:
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Namespace
        name: ${NAMESPACE_ID}
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Stage
        name: 'prod'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    parameterValues:
    - name: STAGE_ID
      value: 'prod'
      toFieldPaths:
        - spec.stageId

