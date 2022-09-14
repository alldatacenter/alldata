--SQL
--********************************************************************--
--Author: opensearch@service.aliyun.com
--CreateTime: 2019-11-25 18:58:05
--Comment: 请输入业务注释信息
--********************************************************************--

CREATE FUNCTION durationAlarm AS 'com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf.DurationAlarm';
CREATE FUNCTION noDataAlarm AS 'com.elasticsearch.cloud.monitor.metric.alarm.blink.udaf.NoDataAlarm';
CREATE FUNCTION splitEventList AS 'com.elasticsearch.cloud.monitor.metric.alarm.blink.udtf.splitEventList';

create table metric_stream (
  rule_id BIGINT  ,
  metric_name VARCHAR,
  metric_value DOUBLE,
  `timestamp` BIGINT,
  tags VARCHAR,
  tenant VARCHAR,
  granularity VARCHAR, -- 20s,1m,10m,1h
  ts AS TO_TIMESTAMP (`timestamp`)
  ,WATERMARK FOR ts AS WITHOFFSET (ts, 60000)
  )
with (
 type='sls',
 endPoint='',
 accessId='',
 accessKey='',
 project='',
 logstore='',
 consumerGroup = 'es-monitor-alarm',
 batchGetSize = '1000'
);

CREATE table alarm_event (
  operator VARCHAR ,
  service VARCHAR , --应用名
  source VARCHAR ,
  tag VARCHAR ,
  text VARCHAR , --报警内容
  title VARCHAR , --报警名
  type VARCHAR , --报警级别 alert-warning,alert-critical
  `time` VARCHAR,
  `group` VARCHAR,
   uid VARCHAR
  )
with (
 type='sls',
 endPoint='',
accessId='',
 accessKey='',
 project='',
 logstore=''
 );

----------------------------
CREATE VIEW metric_filter AS
SELECT
  *
FROM
  metric_stream
WHERE
--   (UNIX_TIMESTAMP()*1000 - `timestamp`) < 10*60*1000 AND --udtf里面可以汇报数据延迟??
    granularity = '1m'; -- 一个job只处理一个精度的数据, 比较简单

----------------------------
INSERT INTO alarm_event
SELECT
  'alarm_job',
  E.service,
  E.source,
  E.tag,
  E.text,
  E.title,
  E.type,
  E.`time`,
  E.`group`,
  E.uid
FROM  metric_filter,
      lateral table (durationAlarm(rule_id,metric_name,`timestamp`,metric_value,tags, granularity, tenant))
        as E (service,source,tag,text,title,type,`time`,`group`,uid);

----------------------------
CREATE VIEW noDataAlarm_view(eventArray) AS
SELECT
  noDataAlarm(rule_id, `timestamp`, tenant, tags)
FROM
  metric_filter
GROUP BY --当上游数据不均匀的时候, 可能会造成window结束不了, 没有结果输出
  TUMBLE (ts, INTERVAL '1' SECOND),tenant;-- 上游agg的数据, 在一个窗口内必须都是相同秒数上的

INSERT INTO alarm_event
SELECT
  'alarm_job',
  E.service,
  E.source,
  E.tag,
  E.text,
  E.title,
  E.type,
  E.`time`,
  E.`group`,
  E.uid
FROM  noDataAlarm_view as N,
  lateral table (splitEventList(N.eventArray)) as E (service,source,tag,text,title,type,`time`,`group`,uid);