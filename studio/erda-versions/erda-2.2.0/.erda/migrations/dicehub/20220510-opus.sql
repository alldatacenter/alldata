CREATE TABLE `erda_release_opus`
(
    `id`              VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`          BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'updater user id',

    `release_id`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'dice_release.release_id',
    `opus_id`         VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus.id',
    `opus_version_id` VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',

    INDEX             idx_org_id (`org_id`),
    INDEX             idx_release_id (`release_id`),
    INDEX             idx_opus_id (`opus_id`),
    INDEX             idx_version_id (`opus_version_id`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT '项目制品上架记录表';