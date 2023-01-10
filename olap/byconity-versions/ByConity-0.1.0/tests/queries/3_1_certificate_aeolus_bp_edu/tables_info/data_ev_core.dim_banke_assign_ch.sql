CREATE DATABASE IF NOT EXISTS data_ev_core;
DROP TABLE IF EXISTS data_ev_core.dim_banke_assign_ch;
CREATE TABLE data_ev_core.dim_banke_assign_ch (`date` Date ,`assign_id` Int8 ,`banke_id` Int64 ,`assign_name` String ) ENGINE = CnchMergeTree() PARTITION BY date ORDER BY (banke_id,assign_id,intHash64(banke_id)) SAMPLE BY intHash64(banke_id);
INSERT INTO data_ev_core.dim_banke_assign_ch FORMAT CSV INFILE '/data01/xinghe.shangguan/cnch-sql-cases/tools/certificate_builder/certificate_aeolus_bp_edu/tables_info/data_ev_core.dim_banke_assign_ch.csv' SETTINGS input_format_skip_unknown_fields = 1, skip_nullinput_notnull_col = 1;
