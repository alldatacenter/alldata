CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.app_business_market_article_peiyu_di;
CREATE TABLE dzx.app_business_market_article_peiyu_di (`new_a3_cnt` Int64 ,`article_type` Int64 ,`p_date` String ,`group_id` String ,`project_id` Int64 ,`new_a2_cnt` Int64 ,`plan_id` Int64 ,`series_id` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (project_id,series_id,article_type,intHash64(series_id)) SAMPLE BY intHash64(series_id);
INSERT INTO dzx.app_business_market_article_peiyu_di FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.app_business_market_article_peiyu_di.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
