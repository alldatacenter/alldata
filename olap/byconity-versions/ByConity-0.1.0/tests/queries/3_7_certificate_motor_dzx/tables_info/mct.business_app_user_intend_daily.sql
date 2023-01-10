CREATE DATABASE IF NOT EXISTS mct;
DROP TABLE IF EXISTS mct.business_app_user_intend_daily;
CREATE TABLE mct.business_app_user_intend_daily (`type_name` String ,`p_date` String ,`type_id` Int64 ,`profile_label_value` String ,`user_level` Int64 ,`type` String ,`profile_label_name` String ,`intention` String ,`user_number` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (type_id,profile_label_value,user_level,intHash64(type_id)) SAMPLE BY intHash64(type_id);
INSERT INTO mct.business_app_user_intend_daily FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/mct.business_app_user_intend_daily.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
