CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.app_business_mental_prefro_di;
CREATE TABLE dzx.app_business_mental_prefro_di (`cart_id` String ,`emotion_tag` String ,`emotion_level1` String ,`account_id` Int64 ,`p_date` String ,`account_name` String ,`series_name` String ,`type` Int64 ,`type_value` Float64 ,`series_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (type,emotion_level1,account_id,series_id,cart_id,intHash64(series_id)) SAMPLE BY intHash64(series_id);
INSERT INTO dzx.app_business_mental_prefro_di FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.app_business_mental_prefro_di.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
