CREATE TABLE `application_alias`
(
    `user_id`     BIGINT(20),
    `user_name`   VARCHAR(256),
    `id3`         BIGINT(20),
    `col4`        VARCHAR(256),
    `col5`        VARCHAR(256),
    `col6`        VARCHAR(256)
 , PRIMARY KEY (`user_id`,`id3`,`col6`)
)
 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
