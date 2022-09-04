apiVersion: v1
kind: Deployment
metadata:
  displayName: push-pmdb-metric-data
  labels: {}
  name: push-pmdb-metric-data
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

          INSERT INTO `vvp`.`${VVP_WORK_NS}`.`sink_pmdb_metric_instance_mysql`
          SELECT
            v.`uid`,
            v.`metric_id`,
            v.`labels`
          FROM  `vvp`.`${VVP_WORK_NS}`.`pmdb_metric_data_view` v
          ;

          INSERT INTO `vvp`.`${VVP_WORK_NS}`.`sink_dwd_metric_data_es`
          SELECT
            GeneratePmdbMetricDataIdUdf(v.`metric_id`, v.`labels`, v.`timestamp`) as `id`,
            v.`uid`,
            v.`metric_id`,
            v.`metric_name`,
            ConvertStr2JSONUdf(v.`labels`) as `labels`,
            v.`value`,
            v.`type`,
            v.`timestamp`,
            FROM_UNIXTIME(v.`timestamp`/1000, 'yyyyMMdd') as `ds`
          FROM  `vvp`.`${VVP_WORK_NS}`.`pmdb_metric_data_view` v
          ;

          INSERT INTO `vvp`.`${VVP_WORK_NS}`.`sink_sreworks_metric_data_kafka`
          SELECT
            v.`uid`,
            v.`metric_id` as `metricId`,
            v.`metric_name` as `metricName`,
            ConvertStr2JSONUdf(v.`labels`) as `labels`,
            v.`value`,
            v.`type`,
            v.`timestamp`
          FROM  `vvp`.`${VVP_WORK_NS}`.`pmdb_metric_data_view` v
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

