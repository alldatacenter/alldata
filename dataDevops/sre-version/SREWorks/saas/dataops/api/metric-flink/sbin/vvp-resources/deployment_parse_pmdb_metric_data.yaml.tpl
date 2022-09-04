apiVersion: v1
kind: Deployment
metadata:
  displayName: parse-pmdb-metric-data
  labels: {}
  name: parse-pmdb-metric-data
  namespace: ${VVP_WORK_NS}
  resourceVersion: 20
spec:
  maxJobCreationAttempts: 4
  maxSavepointCreationAttempts: 4
  restoreStrategy:
    allowNonRestoredState: false
    kind: LATEST_STATE
  sessionClusterName: sreworks-session-cluster
  state: RUNNING
  template:
    spec:
      artifact:
        additionalDependencies:
          - >-
            ${jar_uri}
        flinkVersion: '1.14'
        kind: SQLSCRIPT
        sqlScript: |-
          BEGIN STATEMENT SET;

          INSERT INTO `vvp`.`${VVP_WORK_NS}`.`sink_sreworks_pmdb_parsed_metric_kafka`
          SELECT
            v.`metric_id`,
            v.`labels`,
            v.`value`,
            v.`timestamp`
          FROM  `vvp`.`${VVP_WORK_NS}`.`pmdb_parsed_metric_view` v
          ;

          END;
      flinkConfiguration:
        execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
        execution.checkpointing.interval: 10s
        execution.checkpointing.min-pause: 10s
        high-availability: vvp-kubernetes
        metrics.reporters: prom
        metrics.reporter.prom.port: '9249'
        metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
        state.backend: filesystem
        taskmanager.memory.managed.fraction: '0.0'
        execution.checkpointing.checkpoints-after-tasks-finish.enabled: true
      parallelism: 1
  upgradeStrategy:
    kind: STATEFUL

