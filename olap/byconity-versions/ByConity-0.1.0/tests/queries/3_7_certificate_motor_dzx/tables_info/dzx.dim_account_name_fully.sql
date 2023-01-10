CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.dim_account_name_fully;
CREATE TABLE dzx.dim_account_name_fully (`car_series_type` String ,`sub_brand_id` Int64 ,`account_id` Int64 ,`p_date` Date ,`account_name` String ,`series_name` String ,`brand_name` String ,`sub_brand_name` String ,`series_id` Int64 ,`brand_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (account_id,series_id,intHash64(series_id)) SAMPLE BY intHash64(series_id);
INSERT INTO dzx.dim_account_name_fully FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.dim_account_name_fully.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
