CREATE DATABASE IF NOT EXISTS mct;
DROP TABLE IF EXISTS mct.business_dwa_user_5a_flow_daily;
CREATE TABLE mct.business_dwa_user_5a_flow_daily (`car_series_type` String ,`type_name` String ,`user_level_one` String ,`device_id` BitMap64,`p_date` Date ,`shard_id` Int64 ,`type_id` Int64 ,`user_level_two` String ,`type` String ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (type_id,intHash64(shard_id)) SAMPLE BY intHash64(shard_id);
INSERT INTO mct.business_dwa_user_5a_flow_daily FORMAT CSV INFILE '/data01/jiaxiang.yu/cnch-sql-cases/regression_correctness_check/certificate_motor_dzx/tables_info/mct.business_dwa_user_5a_flow_daily.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
