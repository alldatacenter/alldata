-- MIGRATION_BASE

CREATE TABLE `dice_ucevent_sync_record`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT,
    `created_at`   datetime    DEFAULT NULL COMMENT '表记录创建时间',
    `updated_at`   datetime    DEFAULT NULL COMMENT '表记录更新时间',
    `uc_id`        bigint(20) NOT NULL COMMENT 'uc事件id',
    `uc_eventtime` datetime NOT NULL COMMENT 'uc事件时间',
    `un_receiver`  varchar(40) DEFAULT NULL COMMENT 'uc事件同步失败的接收者',
    PRIMARY KEY (`id`),
    KEY            `idx_uc_id` (`uc_id`),
    KEY            `idx_uc_eventtime` (`uc_eventtime`),
    KEY            `idx_un_receiver` (`un_receiver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='dice拉取uc事件的记录';

