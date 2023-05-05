CREATE TABLE `erda_gallery_opus`
(
    `id`                 VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`         DATETIME     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`             BIGINT(20)   NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'updater user id',

    `level`              VARCHAR(32)  NOT NULL default 'org' COMMENT 'opus 级别: sys, org',
    `type`               VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'opus 类型',
    `name`               VARCHAR(191) NOT NULL DEFAULT '' COMMENT 'opus 标识',
    `display_name`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'opus 名称',
    `display_name_i18n`  TEXT         NOT NULL COMMENT '多语言 opus 名称, JSON 结构',
    `summary`            VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'opus 概述',
    `summary_i18n`       TEXT         NOT NULL COMMENT '多语言 opus 概述, JSON 结构',
    `logo_url`           VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'info.logURL: logo 链接',
    `catalog`            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '.catalog: 分目',

    `default_version_id` VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',
    `latest_version_id`  VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',

    UNIQUE KEY uk_org_name_type (`org_id`, `type`, `name`, `deleted_at`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT 'opus 表';


CREATE TABLE `erda_gallery_opus_version`
(
    `id`              VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`          BIGINT(20)   NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'updater user id',

    `opus_id`         VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus.id',

    `version`         VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'opus 版本',
    `summary`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'opus 概述',
    `summary_i18n`    TEXT         NOT NULL COMMENT '多语言 opus 概述, JSON 结构',
    `labels`          VARCHAR(512) NOT NULL DEFAULT '' COMMENT '.labels: 标签, 形如 a=A&b=B 的键值对列表',
    `logo_url`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'info.logURL: logo 链接',
    `check_valid_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '检查有效性链接',
    `is_valid`        BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '有效性标识',

    INDEX idx_opus_id (`opus_id`),
    UNIQUE KEY uk_opus_id_version (`opus_id`, `version`, `deleted_at`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT 'opus 版本';


CREATE TABLE `erda_gallery_opus_presentation`
(
    `id`                VARCHAR(36)   NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        DATETIME      NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`            BIGINT(20)    NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`          VARCHAR(50)   NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id`        VARCHAR(255)  NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id`        VARCHAR(255)  NOT NULL DEFAULT '' COMMENT 'updater user id',

    `opus_id`           VARCHAR(36)   NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus.id',
    `version_id`        VARCHAR(36)   NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',

    `ref`               VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.ref: 展示信息时的回调, 是实现了特定接口的 http 链接, 这个接口要返回展示信息的结构',
    `desc`              VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '.info.desc: opus 描述 [预留字段]',
    `desc_i18n`         TEXT          NOT NULL COMMENT '多语言 opus 描述, JSON 结构',
    `contact_name`      VARCHAR(191)  NOT NULL DEFAULT '' COMMENT '.info.contact.name: 联系人名称',
    `contact_url`       VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.info.contact.url: 联系人链接',
    `contact_email`     VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '.info.contact.email: 联系人 email',
    `is_open_sourced`   BOOLEAN       NOT NULL DEFAULT 0 COMMENT '.info.opensource.isOpenSourced: 是否开源',
    `opensource_url`    VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.info.opensource.url: 开源仓库地址',
    `license_name`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '.info.opensource.license.name: 开源协议, 如 "Apache 2.0"',
    `license_url`       VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.info.opensource.license.url: 开源协议地址, 如 "https://www.apache.org/licenses/LICENSE-2.0.html"',
    `homepage_name`     VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '.info.homepage.name: 主页名称',
    `homepage_url`      VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.info.homepage.url: 主页地址',
    `homepage_logo_url` VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.info.homepage.logURL: 主页 logo 链接',
    `is_downloadable`   BOOLEAN       NOT NULL DEFAULT 0 COMMENT '.download.downloadable: 是否可以下载',
    `download_url`      VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '.download.url: 下载地址',
    `parameters`        MEDIUMTEXT    NOT NULL COMMENT '有效的 Openapi3 协议 Parameter 片段的集合, 用于展示参数, 如 {"ins":"", "parameters": [{}]}',
    `forms`             MEDIUMTEXT    NOT NULL COMMENT '表单结构',
    `i18n`              MEDIUMTEXT    NOT NULL COMMENT 'i18n 信息',

    INDEX idx_opus_id (`opus_id`),
    INDEX idx_version_id (`version_id`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT '如何展示 opus 的信息';


CREATE TABLE `erda_gallery_opus_readme`
(
    `id`         VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`     BIGINT(20)   NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'updater user id',

    `opus_id`    VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus.id',
    `version_id` VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',

    `lang`       VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '语言标识, 如 zh, zh_CN, en, en_US, ',
    `lang_name`  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '语言名称, 如 中文, English',
    `text`       LONGTEXT     NOT NULL COMMENT 'readme 文本',

    INDEX idx_opus_id (`opus_id`),
    INDEX idx_version_id (`version_id`),
    UNIQUE KEY uk_version_lang (`version_id`, `lang`, `deleted_at`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT 'opus 的 readme 信息';


CREATE TABLE `erda_gallery_opus_installation`
(
    `id`         VARCHAR(36)  NOT NULL PRIMARY KEY COMMENT 'primary key',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME     NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '删除时间',

    `org_id`     BIGINT(20)   NOT NULL DEFAULT 0 COMMENT 'org id',
    `org_name`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT 'org name',
    `creator_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'creator user id',
    `updater_id` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'updater user id',

    `opus_id`    VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus.id',
    `version_id` VARCHAR(36)  NOT NULL DEFAULT '' COMMENT 'erda_gallery_opus_version.id',

    `installer`  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '安装引擎',
    `spec`       MEDIUMTEXT   NOT NULL COMMENT '安装描述文本',

    INDEX idx_opus_id (`opus_id`),
    INDEX idx_version_id (`version_id`)
) DEFAULT CHARACTER SET UTF8MB4 COMMENT 'opus 的安装信息';
