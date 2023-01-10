CREATE DATABASE IF NOT EXISTS dzx;
DROP TABLE IF EXISTS dzx.dim_account_pk_info_fully;
CREATE TABLE dzx.dim_account_pk_info_fully (`pk_rank` Int64 ,`compete_name` String ,`account_id` Int64 ,`distance` Int64 ,`p_date` Date ,`account_name` String ,`compete_account_id` Int64 ,`pk_cnt` Int64 ) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (account_id,intHash64(account_id)) SAMPLE BY intHash64(account_id);
INSERT INTO dzx.dim_account_pk_info_fully FORMAT CSV INFILE '/data01/liulanyi/cnch-sql-cases/tools/certificate_builder/certificate_motor_dzx/tables_info/dzx.dim_account_pk_info_fully.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
