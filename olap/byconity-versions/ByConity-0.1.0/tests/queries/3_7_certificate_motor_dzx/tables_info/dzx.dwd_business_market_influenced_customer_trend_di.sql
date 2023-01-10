CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.dwd_business_market_influenced_customer_trend_di;
CREATE TABLE dzx.dwd_business_market_influenced_customer_trend_di (`is_high_clue` Int64 ,`device_id` String ,`p_date` String ,`project_id` Int64 ,`series_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (series_id,project_id,cityHash64(device_id)) SAMPLE BY cityHash64(device_id);
INSERT INTO dzx.dwd_business_market_influenced_customer_trend_di FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.dwd_business_market_influenced_customer_trend_di.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
