CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.dwa_business_zxt_base_groupid_distribute_di;
CREATE TABLE dzx.dwa_business_zxt_base_groupid_distribute_di (`gtype` String ,`account_id` Int64 ,`p_date` String ,`group_id` String ,`project_id` Int64 ,`series_name` String ,`touch_spot` String ,`plan_id` Int64 ,`series_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (series_id,intHash64(series_id)) SAMPLE BY intHash64(series_id);
INSERT INTO dzx.dwa_business_zxt_base_groupid_distribute_di FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.dwa_business_zxt_base_groupid_distribute_di.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
