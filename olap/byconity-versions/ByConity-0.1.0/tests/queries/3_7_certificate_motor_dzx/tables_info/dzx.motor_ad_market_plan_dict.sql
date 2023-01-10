CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.motor_ad_market_plan_dict;
CREATE TABLE dzx.motor_ad_market_plan_dict (`plan_start_time` Int64 ,`p_date` String ,`project_id` Int64 ,`plan_strategy` Int64 ,`plan_kpi` Int64 ,`id` Int64 ,`plan_end_time` Int64 ,`plan_name` String ,`plan_id` Int64 ,`series_id` Int64 ,`brand_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (plan_id,intHash64(plan_id)) SAMPLE BY intHash64(plan_id);
INSERT INTO dzx.motor_ad_market_plan_dict FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.motor_ad_market_plan_dict.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
