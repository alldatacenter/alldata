CREATE DATABASE IF NOT EXISTS cnch_alpha_lq_audit_arm;
DROP TABLE IF EXISTS cnch_alpha_lq_audit_arm.device_user_relate;
CREATE TABLE IF NOT EXISTS cnch_alpha_lq_audit_arm.device_user_relate (`device_id` String, `p_date` Date, `user_id` Int64, `mc` String, `udid` String) ENGINE = CnchMergeTree() PARTITION BY p_date ORDER BY (device_id, intHash64(user_id)) SAMPLE BY intHash64(user_id) SETTINGS index_granularity = 8192;
INSERT INTO cnch_alpha_lq_audit_arm.device_user_relate FORMAT CSV INFILE '/data01/jiaxiang.yu/cnch-sql-cases/tools/certificate_builder/certificate_datarocks/tables_info/cnch_alpha_lq_audit_arm.device_user_relate.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
