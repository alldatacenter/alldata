CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.app_business_user_5a_c806_res_daily;
CREATE TABLE dzx.app_business_user_5a_c806_res_daily (`ab_res` Int32 ,`series_id` Int64 ,`score` Float64 ,`p_date` String ,`data_type` Int32 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (series_id,intHash64(series_id)) SAMPLE BY intHash64(series_id);
INSERT INTO dzx.app_business_user_5a_c806_res_daily FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.app_business_user_5a_c806_res_daily.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
