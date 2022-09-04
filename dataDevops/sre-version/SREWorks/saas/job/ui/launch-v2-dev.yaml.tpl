apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-job-package
  annotations:
    appId: job
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
        path: "/sreworks-job/**"
        servicePort: 80
        serviceName: "prod-job-job-master"
        routeId: "dev-job-job-master-master-${NAMESPACE_ID}-dev"

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
