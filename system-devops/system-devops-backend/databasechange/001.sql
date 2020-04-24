#字典
CREATE TABLE dict
(
  id          INT AUTO_INCREMENT,
  create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dict_name   varchar(32) not null,
  dict_value  varchar(64) not null,
  dict_desc   varchar(64) not null,
  PRIMARY KEY (id),
  UNIQUE key (dict_name, dict_value)
)
  ENGINE = InnoDB
  CHARACTER SET utf8;


CREATE TABLE original_game
(
  id          INT AUTO_INCREMENT,
  create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  name        varchar(32) not null,
  PRIMARY KEY (id)
) ENGINE = InnoDB
  CHARACTER SET utf8;


CREATE TABLE original_level1
(
  id               INT AUTO_INCREMENT,
  create_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  original_game_id Int         not null,
  name             varchar(32) not null,
  PRIMARY KEY (id)
) ENGINE = InnoDB
  CHARACTER SET utf8;

CREATE TABLE original_level2
(
  id                 INT AUTO_INCREMENT,
  create_time        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  original_level1_id Int         not null,
  name               varchar(32) not null,
  PRIMARY KEY (id)
) ENGINE = InnoDB
  CHARACTER SET utf8;

-- CREATE TABLE original_model(
--   id          INT                AUTO_INCREMENT,
--   create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--   update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--   original_level2_id Int not null,
--   name   varchar(32) not null,
--   version varchar(32) not null,
--   operator varchar(32) not null,
--   file_type varchar(32) not null,
--   oss_url varchar(256) not null,
--   PRIMARY KEY (id)
-- ) ENGINE = InnoDB
--   CHARACTER SET utf8;

CREATE TABLE `favorites`
(
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `favorite`    varchar(255)  NOT NULL,
  `description` varchar(1024) NOT NULL,
  `username`    varchar(32)   NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET utf8;

CREATE TABLE `material`
(
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `create_time` timestamp                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `item_id`     varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `favorite_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET utf8;

CREATE TABLE `video_content_task`
(
  `id`           int(10) unsigned NOT NULL AUTO_INCREMENT,
  `create_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `aepPath`      varchar(512)  NOT NULL DEFAULT '',
  `name`         varchar(512)  NOT NULL DEFAULT '',
  `status`       varchar(64)   NOT NULL DEFAULT '',
  `videoTime`    varchar(64)   NOT NULL DEFAULT '',
  `finalTime`    varchar(64)   NOT NULL,
  `endingTime`   varchar(64)   NOT NULL,
  `preViewPath`  varchar(512)  NOT NULL DEFAULT '',
  `resultPath`   varchar(1024) NOT NULL DEFAULT '',
  `pixel_length` varchar(64)   NOT NULL,
  `pixel_height` varchar(64)   NOT NULL,
  `frame_rate`   varchar(64)   NOT NULL,
  `language`     varchar(255)  NOT NULL,
  `videoValue`   varchar(1024) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `video_ending_task`
(
  `id`           int(10) unsigned NOT NULL AUTO_INCREMENT,
  `create_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `aepPath`      varchar(512)  NOT NULL DEFAULT '',
  `name`         varchar(512)  NOT NULL DEFAULT '',
  `videoTime`    varchar(64)   NOT NULL DEFAULT '',
  `videoValue`   varchar(512)  NOT NULL DEFAULT '',
  `preViewPath`  varchar(512)  NOT NULL DEFAULT '',
  `resultPath`   varchar(1024) NOT NULL DEFAULT '',
  `pixel_length` varchar(64)   NOT NULL,
  `pixel_height` varchar(64)   NOT NULL,
  `frame_rate`   varchar(64)   NOT NULL,
  `status`       varchar(64)   NOT NULL,
  `language`     varchar(255)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `video_final_task`
(
  `id`           int(10) unsigned NOT NULL AUTO_INCREMENT,
  `create_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name`         varchar(255)  NOT NULL,
  `id_content`   varchar(10)   NOT NULL,
  `id_ending`    varchar(10)   NOT NULL,
  `status`       varchar(64)   NOT NULL DEFAULT '',
  `videoTime`    varchar(64)   NOT NULL DEFAULT '',
  `preViewPath`  varchar(512)  NOT NULL DEFAULT '',
  `resultPath`   varchar(1024) NOT NULL DEFAULT '',
  `videoValue`   varchar(512)  NOT NULL DEFAULT '',
  `creator`      varchar(64)   NOT NULL DEFAULT '',
  `urlEnding`    varchar(1024) NOT NULL,
  `pixel_length` varchar(64)   NOT NULL,
  `pixel_height` varchar(64)   NOT NULL,
  `frame_rate`   varchar(64)   NOT NULL,
  `language`     varchar(255)  NOT NULL,
  `urlContent`   varchar(2048) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;