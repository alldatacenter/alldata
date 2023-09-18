CREATE EXTERNAL TABLE IF NOT EXISTS `application`
(
    `user_id`     BIGINT,
    `user_name`   STRING,
    `id3`         BIGINT,
    `col4`        STRING,
    `col5`        STRING,
    `col6`        STRING
)
COMMENT 'tis_tmp_application' PARTITIONED BY(pt string,pmod string)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe' with SERDEPROPERTIES ('serialization.null.format'='', 'line.delim' ='
','field.delim'='')
STORED AS TEXTFILE
LOCATION '/user/admin/tis/application'
