apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-system-package
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
    revisionName: INTERNAL_ADDON|productops|_
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
      value: "http://prod-sreworks-flycore-paas-productops"
      toFieldPaths:
        - spec.targetEndpoint
    - name: STAGE_ID
      value: 'prod'
      toFieldPaths:
        - spec.stageId
