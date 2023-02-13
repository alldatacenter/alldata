apiVersion: v1
kind: Deployment
metadata:
  displayName: health-failure
  labels: {}
  name: health-failure
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

          INSERT INTO `vvp`.`${VVP_WORK_NS}`.`print_failure_instance`
          SELECT
          E.failure_def_id AS def_id
          FROM  `vvp`.`${VVP_WORK_NS}`.`health_failure_incident_view` v1,
          lateral table (HealthFailure(v1.`incidentInstanceId`, v1.appInstanceId, v1.appComponentInstanceId, v1.gmtOccur, v1.gmtRecovery, v1.cause, v1.failure_def_id, v1.`app_name`, v1.ex_config)) as
          E (failure_def_id, app_instance_id, app_component_instance_id, incident_instance_id, failure_instance_name, failure_level, occur_ts, recovery_ts, content)
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

