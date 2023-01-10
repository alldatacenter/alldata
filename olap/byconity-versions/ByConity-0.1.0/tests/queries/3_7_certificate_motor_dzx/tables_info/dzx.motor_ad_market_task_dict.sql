CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.motor_ad_market_task_dict;
CREATE TABLE dzx.motor_ad_market_task_dict (`status` Int64 ,`update_time` Int64 ,`is_deleted` Int64 ,`task_id` Int64 ,`extra` String ,`task_kpi` Int64 ,`p_date` String ,`create_time` Int64 ,`date` String ,`plan_id` Int64 ,`project_id` Int64 ,`id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (task_id,plan_id,project_id,intHash64(task_id)) SAMPLE BY intHash64(task_id);
INSERT INTO dzx.motor_ad_market_task_dict FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.motor_ad_market_task_dict.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
