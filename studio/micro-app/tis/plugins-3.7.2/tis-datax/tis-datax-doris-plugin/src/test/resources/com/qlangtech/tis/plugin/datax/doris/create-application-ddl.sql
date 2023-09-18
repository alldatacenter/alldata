CREATE TABLE application
(
    `user_id`     BIGINT NOT NULL,
    `id3`         BIGINT NOT NULL,
    `col6`        VARCHAR(256) NOT NULL,
    `user_name`   VARCHAR(256),
    `col4`        VARCHAR(256),
    `col5`        VARCHAR(256)
)
 ENGINE=olap
PRIMARY KEY(`user_id`,`id3`,`col6`)
DISTRIBUTED BY HASH(`user_id`,`id3`,`col6`)
BUCKETS 10
PROPERTIES("replication_num" = "1")
