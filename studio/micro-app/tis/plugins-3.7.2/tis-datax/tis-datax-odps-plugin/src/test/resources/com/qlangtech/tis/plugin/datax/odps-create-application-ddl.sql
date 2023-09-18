CREATE TABLE IF NOT EXISTS `application`
(
    `user_id`     BIGINT,
    `user_name`   STRING,
    `id3`         BIGINT,
    `col4`        STRING,
    `col5`        STRING,
    `col6`        STRING
)
COMMENT 'tis_tmp_application' PARTITIONED BY(pt string,pmod string)
