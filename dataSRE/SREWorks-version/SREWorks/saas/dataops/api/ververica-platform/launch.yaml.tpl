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
    revisionName: "HELM|ververica-platform|_"
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
    traits: []
    dependencies: []
    parameterValues:
    - name: "values"
      value:
        acceptCommunityEditionLicense: true
        vvp:
          registry: ${VVP_REGISTRY}
          persistence:
            type: local
        
          blobStorage:
            baseUri: s3://vvp
            s3:
              endpoint: http://${ENDPOINT_PAAS_MINIO}
        
          globalDeploymentDefaults: |
            spec:
              state: RUNNING
              template:
                spec:
                  resources:
                    jobmanager:
                      cpu: 0.5
                      memory: 1G
                    taskmanager:
                      cpu: 0.5
                      memory: 1G
                  flinkConfiguration:
                    state.backend: filesystem
                    taskmanager.memory.managed.fraction: 0.0 # no managed memory needed for filesystem statebackend
                    high-availability: vvp-kubernetes
                    metrics.reporters: prom
                    metrics.reporter.prom.port: '9249'
                    metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
                    execution.checkpointing.interval: 10s
                    execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
        
          sqlService:
            pool:
              coreSize: 1
              maxSize: 1
        
        blobStorageCredentials:
          s3:
            accessKeyId: ${MINIO_ACCESS_KEY}
            secretAccessKey: ${MINIO_SECRET_KEY}

        appmanager:
          repository: "${VVP_APPMANAGER_REPO}"
          tag: "${VVP_APPMANAGER_IMAGE_TAG}"
          resources:
            limits:
              cpu: 500m
              memory: 1Gi
            requests:
              cpu: 250m
              memory: 1Gi

        gateway:
          repository: "${VVP_GATEWAY_REPO}"
          tag: "${VVP_GATEWAY_IMAGE_TAG}" 
          resources:
            limits:
              cpu: 500m
              memory: 1.25Gi
            requests:
              cpu: 250m
              memory: 1.25Gi

        ui:
          repository: "${VVP_UI_REPO}"
          tag: "${VVP_UI_IMAGE_TAG}"
 
        persistentVolume:
          enabled: true
          accessModes:
            - ReadWriteOnce
          annotations: {}
          size: 20Gi
          storageClass: ${GLOBAL_STORAGE_CLASS}
          subPath: ""
        
        #rbac:
        #  additionalNamespaces:
        #    - ${NAMESPACE_ID}


      toFieldPaths:
      - "spec.values"

 
