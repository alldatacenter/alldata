CREATE TABLE `erda_apim_export_record`
(
    `id`              varchar(36)  not null primary key comment 'primary key',
    `created_at`      datetime     not null default current_timestamp comment '创建时间',
    `updated_at`      datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
    `org_id`          bigint       not null default 0 comment '组织 id',
    `org_name`        varchar(50)  not null default '' comment '组织名称',
    `deleted_at`      datetime     not null default '1970-01-01 00:00:00' comment '删除时间',
    `creator_id`      varchar(255) not null default '' comment '创建人 user id',
    `updater_id`      varchar(255) not null default '' comment '更新人 user id',

    `asset_id`        varchar(191) not null default '' comment 'api 集市 id',
    `asset_name`      varchar(191) not null default '' comment 'api 集市名称',
    `version_id`      bigint(20) not null default 0 comment '对应的 dice_api_asset_versions 的主键',
    `swagger_version` varchar(16)  not null default '' comment '用户自定义版本名称',
    `major`           int(11) not null DEFAULT 0 COMMENT 'version major number',
    `minor`           int(11) not null DEFAULT 0 comment 'version minor number',
    `patch`           int(11) not null DEFAULT 0 comment 'version patch number',
    `spec_protocol`   varchar(16)  not null default 'csv' comment '下载文档的协议和格式: oas2-yaml,oas2-json,oas3-yaml,oas3-json,csv'
) engine = InnoDB
  default character set utf8mb4 comment '文档下载记录';
