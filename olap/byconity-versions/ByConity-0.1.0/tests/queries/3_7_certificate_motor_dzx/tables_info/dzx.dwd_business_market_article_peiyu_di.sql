CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.dwd_business_market_article_peiyu_di;
CREATE TABLE dzx.dwd_business_market_article_peiyu_di (`device_id` String ,`p_date` String ,`group_id` String ,`project_id` Int64 ,`is_customize` Int64 ,`label` String ,`plan_id` Int64 ,`series_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (project_id,series_id,cityHash64(device_id)) SAMPLE BY cityHash64(device_id);
INSERT INTO dzx.dwd_business_market_article_peiyu_di FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.dwd_business_market_article_peiyu_di.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
