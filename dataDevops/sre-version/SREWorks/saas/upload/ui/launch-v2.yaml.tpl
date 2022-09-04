apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-upload-package
  annotations:
    appId: upload
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
    - name: TARGET_ENDPOINT
      value: "${CORE_STAGE_ID}-${CORE_APP_ID}-paas-action"
      toFieldPaths:
        - spec.targetEndpoint
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
    - name: OVERWRITE_BUILT_IN
      value: 'true'
      toFieldPaths:
        - spec.overwriteBuiltIn
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

