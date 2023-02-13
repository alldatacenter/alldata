#!/bin/sh

JOB_ROOT=$(cd `dirname $0`; pwd)

while [[ "$(curl -s -o /dev/null -w '%{http_code}\n' http://$VVP_ENDPOINT/swagger)" != "200" ]]
do
    sleep 10
done

###### S3 bucket
$JOB_ROOT/mc alias set sw http://${MINIO_ENDPOINT} ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY}
$JOB_ROOT/mc mb -p sw/vvp

###### CREATE UDF ARTIFACT
echo "============CREATE UDF ARTIFACT============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/udfartifacts \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{"name": "namespaces/'${VVP_WORK_NS}'/udfartifacts/'${UDF_ARTIFACT_NAME}'"}'


###### UPLOAD UDF ARTIFACT
echo "============UPLOAD UDF ARTIFACT============"
upload_response=$(curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/udfartifacts/${UDF_ARTIFACT_NAME}:upload-jar \
    -X POST \
    -F 'file=@/app/sbin/'${UDF_ARTIFACT_JAR})
export jar_uri=$(echo $upload_response | jq -r ".jarUri")


###### REGISTER UDF ARTIFACT
echo "============REGISTER UDF ARTIFACT============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/udfartifacts/${UDF_ARTIFACT_NAME} \
    -X PUT \
    -H 'Content-Type: application/json' \
    -d '{
        "name": "namespaces/'${VVP_WORK_NS}'/udfartifacts/'${UDF_ARTIFACT_NAME}'",
        "jarUrl": "'${jar_uri}'"
    }'


###### REGISTER UDF FUNCTION
echo "============REGISTER UDF FUNCTION============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute-multi?stopOnError=false \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '[
        "CREATE FUNCTION IF NOT EXISTS `NoDataAlarm` AS '\''com.elasticsearch.cloud.monitor.metric.alarm.blink.udaf.NoDataAlarm'\''",
        "CREATE FUNCTION IF NOT EXISTS `ExtractTimeUdf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udf.ExtractTimeUdf'\''",
        "CREATE FUNCTION IF NOT EXISTS `TimeDelay` AS '\''com.elasticsearch.cloud.monitor.metric.alarm.blink.udf.TimeDelay'\''",
        "CREATE FUNCTION IF NOT EXISTS `DurationAlarm` AS '\''com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf.DurationAlarm'\''",
        "CREATE FUNCTION IF NOT EXISTS `HealthAlert` AS '\''com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf.HealthAlert'\''",
        "CREATE FUNCTION IF NOT EXISTS `HealthFailure` AS '\''com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf.HealthFailure'\''",
        "CREATE FUNCTION IF NOT EXISTS `ConvertJSON2StrUdf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udf.ConvertJSON2StrUdf'\''",
        "CREATE FUNCTION IF NOT EXISTS `ConvertStr2JSONUdf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udf.ConvertStr2JSONUdf'\''",
        "CREATE FUNCTION IF NOT EXISTS `MergePmdbMetricLabelsUdtf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udtf.MergePmdbMetricLabelsUdtf'\''",
        "CREATE FUNCTION IF NOT EXISTS `GeneratePmdbMetricDataIdUdf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udf.GeneratePmdbMetricDataIdUdf'\''",
        "CREATE FUNCTION IF NOT EXISTS `ParsePmdbMetricDataUdtf` AS '\''com.elasticsearch.cloud.monitor.metric.common.blink.udtf.ParsePmdbMetricDataUdtf'\''"
    ]'


###### CREATE TABLE
echo "============CREATE TABLE============"

############ parse metric data
echo "============parse metric data============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_telemetry_metric_kafka` (`metric` STRING ) COMMENT '\''可观测指标数据源表'\''WITH ('\''connector'\'' = '\''kafka'\'', '\''format'\'' = '\''raw'\'', '\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'', '\''properties.group.id'\'' = '\''source-sreworks-telemetry-metric-group'\'', '\''scan.startup.mode'\'' = '\''latest-offset'\'', '\''topic'\'' = '\''sreworks-telemetry-metric'\'', '\''value.fields-include'\'' = '\''ALL'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE VIEW `vvp`.`'${VVP_WORK_NS}'`.`pmdb_parsed_metric_view` (`metric_id`, `labels`, `value`, `timestamp` ) COMMENT '\''指标数据解析视图'\''AS SELECT E.metric_id AS metric_id, E.labels AS labels, E.`value` as `value`, E.`timestamp` as `timestamp` FROM  `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_telemetry_metric_kafka` t, lateral table (ParsePmdbMetricDataUdtf(t.metric)) as E (metric_id, labels, `value`, `timestamp`) ;"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`sink_sreworks_pmdb_parsed_metric_kafka` (`metric_id` INT, `labels` STRING, `value` FLOAT, `timestamp` BIGINT ) COMMENT '\''指标数据解析结果表'\''WITH ('\''connector'\'' = '\''kafka'\'', '\''format'\'' = '\''json'\'', '\''json.ignore-parse-errors'\'' = '\''true'\'', '\''json.map-null-key.mode'\'' = '\''DROP'\'', '\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'', '\''properties.group.id'\'' = '\''sink-sreworks-pmdb-parsed-metric-group'\'', '\''topic'\'' = '\''sreworks-pmdb-parsed-metric'\'', '\''value.fields-include'\'' = '\''ALL'\'');"}'


############ push metric data
echo "============push metric data============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_pmdb_parsed_metric_kafka` (`metric_id` INT, `labels` STRING, `value` FLOAT, `timestamp` BIGINT, `proc_time` AS PROCTIME() ) COMMENT '\''指标数据解析源表'\''WITH ('\''connector'\'' = '\''kafka'\'', '\''format'\'' = '\''json'\'', '\''json.fail-on-missing-field'\'' = '\''true'\'', '\''json.ignore-parse-errors'\'' = '\''false'\'', '\''json.map-null-key.mode'\'' = '\''FAIL'\'', '\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'', '\''properties.group.id'\'' = '\''source-sreworks-pmdb-parsed-metric-group'\'', '\''scan.startup.mode'\'' = '\''latest-offset'\'', '\''topic'\'' = '\''sreworks-pmdb-parsed-metric'\'', '\''value.fields-include'\'' = '\''ALL'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`dim_pmdb_metric_mysql` (`id` INT, `name` STRING, `type` STRING, `labels` STRING, PRIMARY KEY (`id`) NOT ENFORCED ) COMMENT '\''指标定义维度表'\''WITH ('\''connector'\'' = '\''jdbc'\'', '\''driver'\'' = '\''org.mariadb.jdbc.Driver'\'', '\''lookup.cache.max-rows'\'' = '\''1000'\'', '\''lookup.cache.ttl'\'' = '\''600s'\'', '\''password'\'' = '\'''${DATA_DB_PASSWORD}''\'', '\''table-name'\'' = '\''metric'\'', '\''url'\'' = '\''jdbc:mysql://'${DATA_DB_HOST}':'${DATA_DB_PORT}'/pmdb?useUnicode=true&characterEncoding=utf-8&useSSL=false'\'', '\''username'\'' = '\'''${DATA_DB_USER}''\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE VIEW `vvp`.`'${VVP_WORK_NS}'`.`pmdb_raw_metric_data_view` (`metric_id`, `metric_name`, `type`, `ins_labels`, `metric_labels`, `timestamp`, `value` ) COMMENT '\''指标数据结果视图'\''AS SELECT t1.metric_id, t2.name as metric_name, t2.type, t1.labels as ins_labels, t2.labels as metric_labels, t1.`timestamp`, t1.`value` FROM `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_pmdb_parsed_metric_kafka` AS t1 INNER JOIN `vvp`.`'${VVP_WORK_NS}'`.`dim_pmdb_metric_mysql` FOR SYSTEM_TIME AS OF t1.`proc_time` AS t2 ON t2.`id` = t1.metric_id;"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE VIEW `vvp`.`'${VVP_WORK_NS}'`.`pmdb_metric_data_view` (`uid`, `metric_id`, `metric_name`, `type`, `labels`, `value`, `timestamp` ) COMMENT '\''指标数据解析视图'\''AS SELECT E.uid, v.metric_id, v.metric_name, v.type, E.labels, v.`value`, v.`timestamp` FROM  `vvp`.`'${VVP_WORK_NS}'`.`pmdb_raw_metric_data_view` v, lateral table (MergePmdbMetricLabelsUdtf(v.metric_id, v.ins_labels, v.metric_labels)) as E (uid, labels) ;"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`sink_pmdb_metric_instance_mysql` (`uid` STRING, `metric_id` INT, `labels` STRING, PRIMARY KEY (`uid`) NOT ENFORCED ) COMMENT '\''指标定义实例结果表'\''WITH ('\''connector'\'' = '\''jdbc'\'', '\''driver'\'' = '\''org.mariadb.jdbc.Driver'\'', '\''lookup.cache.max-rows'\'' = '\''1000'\'', '\''lookup.cache.ttl'\'' = '\''600s'\'', '\''password'\'' = '\'''${DATA_DB_PASSWORD}''\'', '\''table-name'\'' = '\''metric_instance'\'', '\''url'\'' = '\''jdbc:mysql://'${DATA_DB_HOST}':'${DATA_DB_PORT}'/pmdb?useUnicode=true&characterEncoding=utf-8&useSSL=false'\'', '\''username'\'' = '\'''${DATA_DB_USER}''\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`sink_dwd_metric_data_es` (`id` STRING, `uid` STRING, `metric_id` INT, `metric_name` STRING, `labels` MAP<STRING, STRING>, `value` FLOAT, `type` STRING, `timestamp` BIGINT, `ds` STRING, PRIMARY KEY (`id`) NOT ENFORCED ) COMMENT '\''数仓指标数据结果表'\''WITH ('\''connector'\'' = '\''elasticsearch-7'\'', '\''hosts'\'' = '\''http://'${DATA_ES_HOST}':'${DATA_ES_PORT}''\'', '\''index'\'' = '\''dwd_original_metric_data_di'\'', '\''username'\'' = '\'''${DATA_ES_USER}''\'', '\''password'\'' = '\'''${DATA_ES_PASSWORD}''\'', '\''format'\'' = '\''json'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`sink_sreworks_metric_data_kafka` (`uid` STRING, `metricId` INT, `metricName` STRING, `labels` MAP<STRING, STRING>, `value` FLOAT, `type` STRING, `timestamp` BIGINT ) COMMENT '\''指标数据结果表'\''WITH ('\''connector'\'' = '\''kafka'\'', '\''format'\'' = '\''json'\'', '\''json.ignore-parse-errors'\'' = '\''true'\'', '\''json.map-null-key.mode'\'' = '\''DROP'\'', '\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'', '\''properties.group.id'\'' = '\''sink-sreworks-metric-data-group'\'', '\''topic'\'' = '\''sreworks-dataops-metric-data'\'', '\''value.fields-include'\'' = '\''ALL'\'');"}'


############ health alert
echo "============health alert============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_metric_data_kafka` (`uid` VARCHAR,`metricId` INT,`metricName` VARCHAR,`type` VARCHAR,`labels` MAP<VARCHAR, VARCHAR>,`timestamp` BIGINT,`value` FLOAT,`proc_time` AS PROCTIME()) COMMENT '\''指标数据源表'\'' WITH ('\''connector'\'' = '\''kafka'\'','\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'','\''topic'\'' = '\''sreworks-dataops-metric-data'\'','\''properties.group.id'\'' = '\''sreworks-dataops-metric-flink-group'\'','\''scan.startup.mode'\'' = '\''latest-offset'\'','\''value.fields-include'\'' = '\''ALL'\'','\''format'\'' = '\''json'\'','\''json.map-null-key.mode'\'' = '\''FAIL'\'','\''json.fail-on-missing-field'\'' = '\''true'\'','\''json.ignore-parse-errors'\'' = '\''false'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`dim_health_def_mysql` (id INT, name VARCHAR, category VARCHAR, app_id VARCHAR, app_name VARCHAR, app_component_name VARCHAR, metric_id INT, failure_ref_incident_id INT, ex_config VARCHAR, PRIMARY KEY (metric_id) NOT ENFORCED) COMMENT '\''告警定义维度表'\''WITH ('\''connector'\'' = '\''jdbc'\'', '\''driver'\'' = '\''org.mariadb.jdbc.Driver'\'', '\''password'\'' = '\'''${DATA_DB_PASSWORD}''\'', '\''table-name'\'' = '\''common_definition'\'', '\''url'\'' = '\''jdbc:mysql://'${DATA_DB_HOST}':'${DATA_DB_PORT}'/'${DATA_DB_HEALTH_NAME}'?useUnicode=true&characterEncoding=utf-8&useSSL=false'\'', '\''username'\'' = '\'''${DATA_DB_USER}''\'', '\''lookup.cache.max-rows'\'' = '\''1000'\'', '\''lookup.cache.ttl'\'' = '\''600s'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`sink_health_alert_instance_mysql` (`def_id` INT, `app_instance_id` VARCHAR, `app_component_instance_id` VARCHAR, `metric_instance_id` VARCHAR, `metric_instance_labels` VARCHAR, `gmt_occur` TIMESTAMP(3), `source` VARCHAR, `level` VARCHAR, `content` VARCHAR) COMMENT '\''告警记录表'\''WITH ('\''connector'\'' = '\''jdbc'\'', '\''driver'\'' = '\''org.mariadb.jdbc.Driver'\'', '\''password'\'' = '\'''${DATA_DB_PASSWORD}''\'', '\''table-name'\'' = '\''alert_instance'\'', '\''url'\'' = '\''jdbc:mysql://'${DATA_DB_HOST}':'${DATA_DB_PORT}'/'${DATA_DB_HEALTH_NAME}'?useUnicode=true&characterEncoding=utf-8&ueSSL=false'\'', '\''username'\'' = '\'''${DATA_DB_USER}''\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`print_alert_instance` (defId INT) WITH ('\''connector'\'' = '\''print'\'')"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE VIEW `vvp`.`'${VVP_WORK_NS}'`.`metric_data_alert_rule_view` (`uid`,`metricId`,`metricName`,`type`,`labels`,`timestamp`,`value`,`def_id`,`def_name`,`app_id`,`app_name`,`app_component_name`,`ex_config`) COMMENT '\''指标数值告警定义视图'\'' AS SELECT `t1`.`uid`, `t1`.`metricId`, `t1`.`metricName`, `t1`.`type`, `t1`.`labels`, `t1`.`timestamp`, `t1`.`value`, `t2`.`id` AS `def_id`, `t2`.`name` AS `def_name`, `t2`.`app_id`, `t2`.`app_name`, `t2`.`app_component_name`, `t2`.`ex_config` FROM `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_metric_data_kafka` AS t1 INNER JOIN `vvp`.`'${VVP_WORK_NS}'`.`dim_health_def_mysql` FOR SYSTEM_TIME AS OF `t1`.`proc_time` AS t2 ON t2.metric_id=t1.metricId WHERE t2.category='\''alert'\'';"}'


############ health failure
echo "============health failure============"
curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_health_incident_instance_kafka` (`id` BIGINT, `defId` INT, `appInstanceId` VARCHAR, `appComponentInstanceId` VARCHAR, `gmtOccur` BIGINT, `gmtRecovery` BIGINT, `cause` VARCHAR, `proc_time` AS PROCTIME()) COMMENT '\''异常实例数据源表'\'' WITH ('\''connector'\'' = '\''kafka'\'', '\''format'\'' = '\''json'\'', '\''json.fail-on-missing-field'\'' = '\''true'\'', '\''json.ignore-parse-errors'\'' = '\''false'\'', '\''json.map-null-key.mode'\'' = '\''FAIL'\'', '\''properties.bootstrap.servers'\'' = '\''http://'${KAFKA_URL}''\'', '\''properties.group.id'\'' = '\''sreworks-health-incident-flink-group'\'', '\''scan.startup.mode'\'' = '\''latest-offset'\'', '\''topic'\'' = '\''sreworks-health-incident'\'', '\''value.fields-include'\'' = '\''ALL'\'');"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE TABLE `vvp`.`'${VVP_WORK_NS}'`.`print_failure_instance` (defId INT) WITH ('\''connector'\'' = '\''print'\'')"}'

curl http://${VVP_ENDPOINT}/sql/v1beta1/namespaces/${VVP_WORK_NS}/sqlscripts:execute \
   -X POST \
   -H 'Content-Type: application/json' \
   -d '{"statement":"CREATE VIEW `vvp`.`'${VVP_WORK_NS}'`.`health_failure_incident_view` (`incidentInstanceId`, `appInstanceId`, `appComponentInstanceId`, `gmtOccur`, `gmtRecovery`, `cause`, `failure_def_id`, `app_name`, `ex_config`) COMMENT '\''故障异常关联视图'\'' AS SELECT `t1`.`id` AS `incidentInstanceId`, `t1`.`appInstanceId`, `t1`.`appComponentInstanceId`, `t1`.`gmtOccur`, `t1`.`gmtRecovery`, `t1`.cause, `t2`.`id` AS `failure_def_id`, `t2`.`app_name`, `t2`.`ex_config` FROM `vvp`.`'${VVP_WORK_NS}'`.`source_sreworks_health_incident_instance_kafka` AS `t1` INNER JOIN `vvp`.`'${VVP_WORK_NS}'`.`dim_health_def_mysql` FOR SYSTEM_TIME AS OF `t1`.`proc_time` AS `t2` ON `t2`.`failure_ref_incident_id` = `t1`.`defId` WHERE `t2`.`category` = '\''failure'\'';"}'


###### CREATE DEPLOYMENT TARGET
echo "============CREATE DEPLOYMENT TARGET============"
envsubst < ${JOB_ROOT}/vvp-resources/deployment_target.yaml.tpl > ${JOB_ROOT}/vvp-resources/deployment_target.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/deployment-targets \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/deployment_target.yaml


###### CREATE SESSION CLUSTER
echo "============CREATE SESSION CLUSTER============"
envsubst < ${JOB_ROOT}/vvp-resources/session_cluster.yaml.tpl > ${JOB_ROOT}/vvp-resources/session_cluster.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/sessionclusters \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/session_cluster.yaml


###### CHECK SESSION CLUSTER STATUS
echo "============CHECK SESSION CLUSTER STATUS============"
session_cluster_state=""
while [[ "$session_cluster_state" != "RUNNING" ]]
do
    echo "check session cluster state: "$session_cluster_state", wait..."
    sleep 10
    session_cluster_info=$(curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/sessionclusters/sreworks-session-cluster -X GET)
    session_cluster_state=$(echo $session_cluster_info | jq -r ".status.state")
done
echo "session cluster is "$session_cluster_state


###### CREATE DEPLOYMENT
echo "============CREATE ALERT DEPLOYMENT============"
envsubst < ${JOB_ROOT}/vvp-resources/deployment_alert.yaml.tpl > ${JOB_ROOT}/vvp-resources/deployment_alert.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/deployments \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/deployment_alert.yaml

echo "============CREATE FAILURE DEPLOYMENT============"
envsubst < ${JOB_ROOT}/vvp-resources/deployment_failure.yaml.tpl > ${JOB_ROOT}/vvp-resources/deployment_failure.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/deployments \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/deployment_failure.yaml


echo "============CREATE PARSED PMDB METRIC DATA DEPLOYMENT============"
envsubst < ${JOB_ROOT}/vvp-resources/deployment_parse_pmdb_metric_data.yaml.tpl > ${JOB_ROOT}/vvp-resources/deployment_parse_pmdb_metric_data.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/deployments \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/deployment_parse_pmdb_metric_data.yaml

echo "============CREATE PUSH PMDB METRIC DATA DEPLOYMENT============"
envsubst < ${JOB_ROOT}/vvp-resources/deployment_push_pmdb_metric_data.yaml.tpl > ${JOB_ROOT}/vvp-resources/deployment_push_pmdb_metric_data.yaml
curl http://${VVP_ENDPOINT}/api/v1/namespaces/${VVP_WORK_NS}/deployments \
    -X POST \
    -H "Content-Type: application/yaml" \
    -H "Accept: application/yaml" \
    --data-binary @/app/sbin/vvp-resources/deployment_push_pmdb_metric_data.yaml
