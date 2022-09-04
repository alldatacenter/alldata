apiVersion: core.oam.dev/v1alpha2
kind: ApplicationConfiguration
spec:
  parameterValues:
    - name: CLUSTER_ID
      value: "master"
    - name: NAMESPACE_ID
      value: "${NAMESPACE_ID}"
    - name: STAGE_ID
      value: "${SAAS_STAGE_ID}"
    - name: ABM_CLUSTER
      value: "default-cluster"
    - name: CLOUD_TYPE
      value: "PaaS"
    - name: ENV_TYPE
      value: "PaaS"
    - name: APP_ID
      value: "dataops"
  components:
  - dataOutputs: []
    revisionName: "HELM|skywalking|_"
    traits: []
    dataInputs: []
    scopes:
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Namespace"
        name: "${NAMESPACE_ID}"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Cluster"
        name: "master"
    - scopeRef:
        apiVersion: "flyadmin.alibaba.com/v1alpha1"
        kind: "Stage"
        name: "${SAAS_STAGE_ID}"
    dependencies: []
    parameterValues:
    - name: "values"
      value:
        initContainer:
          image: "${BUSYBOX_IMAGE}"
          tag: "${BUSYBOX_IMAGE_TAG}"

        oap:
          replicas: 1
          image:
            repository: "${SKYWALKING_OAP_IMAGE}"
            tag: "${SKYWALKING_OAP_IMAGE_TAG}"
          storageType: elasticsearch7
          javaOpts: -Xmx720m -Xms720m

        ui:
          image:
            repository: "${SKYWALKING_UI_IMAGE}"
            tag: "${SKYWALKING_UI_IMAGE_TAG}"

        elasticsearch:
          enabled: false
          config:
            host: "${DATA_ES_HOST}"
            port:
              http: ${DATA_ES_PORT}
            user: "${DATA_ES_USER}"
            password: "${DATA_ES_PASSWORD}"

      toFieldPaths:
      - "spec.values"

 

