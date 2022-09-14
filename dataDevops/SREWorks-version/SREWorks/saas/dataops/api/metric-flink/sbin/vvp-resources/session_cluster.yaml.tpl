kind: SessionCluster
apiVersion: v1
metadata:
  name: sreworks-session-cluster
  labels:
    env: prod
  namespace: ${VVP_WORK_NS}
spec:
  state: RUNNING
  deploymentTargetName: sreworksDeploymentTarget
  flinkVersion: 1.14
  flinkImageRegistry: sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror
  flinkImageRepository: flink
  flinkImageTag: 1.14.2-stream1-scala_2.12-java8
  numberOfTaskManagers: 1
  resources:
    jobmanager:
      cpu: 1
      memory: 1g
    taskmanager:
      cpu: 1
      memory: 1g
  flinkConfiguration:
    taskmanager.numberOfTaskSlots: 4
    metrics.reporters: prom
    metrics.reporter.prom.port: '9249'
    metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
  logging:
    loggingProfile: default
    log4jLoggers:
      "": INFO
      org.apache.flink.streaming.examples: DEBUG
  kubernetes:
    pods:
      annotations:
        prometheus.io/scrape: 'true'
        prometheus.io/port: '9249'
      envVars:
      - name: KEY
        value: VALUE
