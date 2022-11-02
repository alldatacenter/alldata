apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
metadata:
  name: deploy-data-package
  annotations:
    appId: dataops
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
        path: /dataset/**
        servicePort: 80
        serviceName: "prod-dataops-dataset.sreworks-dataops"
        routeId: "dev-dataops-dataset-master-sreworks-dataops-dev"
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
    traits:
    - name: gateway.trait.abm.io
      runtime: post
      spec:
        path: /warehouse/**
        servicePort: 80
        serviceName: "prod-dataops-warehouse.sreworks-dataops"
        routeId: "dev-dataops-warehouse-master-sreworks-dataops-dev"
    parameterValues:
    - name: STAGE_ID
      value: 'dev'
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
        name: 'dev'
    - scopeRef:
        apiVersion: flyadmin.alibaba.com/v1alpha1
        kind: Cluster
        name: 'master'
    traits:
    - name: gateway.trait.abm.io
      runtime: post
      spec:
        path: /pmdb/**
        servicePort: 80
        serviceName: "prod-dataops-pmdb.sreworks-dataops"
        routeId: "dev-dataops-pmdb-master-sreworks-dataops-dev"
    parameterValues:
    - name: STAGE_ID
      value: 'dev'
      toFieldPaths:
        - spec.stageId
