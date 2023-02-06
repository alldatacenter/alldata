-- apim 3.19 models changes

-- 修改 dice_api_assets
create table if not exists dice_api_access
(
    id              bigint unsigned auto_increment primary key comment 'primary key',
    asset_id        varchar(191) comment 'asset id',
    asset_name      varchar(191) comment 'asset name' ,
    org_id          bigint comment 'organization id',
    swagger_version varchar(16) comment 'swagger version',
    major           int(11) comment 'version major number',
    minor           int(11) comment 'version minor number',
    project_id      bigint comment 'project id',
    app_id          bigint comment 'application id',
    workspace       varchar(32) null comment 'DEV, TEST, STAGING, PROD',
    endpoint_id     varchar(32) comment 'gateway endpoint id',
    authentication  varchar(32) comment 'api-key, parameter-sign, auth2',
    authorization   varchar(32) comment 'auto, manual',
    addon_instance_id varchar(128) comment 'addon instance id',
    bind_domain     varchar(256) comment 'bind domains',
    creator_id      varchar(191) comment 'creator user id',
    updater_id      varchar(191) comment 'updater user id',
    created_at      datetime comment 'created datetime',
    updated_at      datetime comment 'last updated datetime'
);

-- 创建客户端表
create table if not exists dice_api_clients
(
    id          bigint unsigned auto_increment primary key comment 'primary key',
    org_id      bigint comment 'organization id',
    name        varchar(64) comment 'client name',
    `desc`      varchar(1024) comment 'describe',
    client_id   varchar(32) comment 'client id',
    creator_id  varchar(191) comment 'creator user id',
    updater_id  varchar(191) comment 'updater user id',
    created_at  datetime comment 'create datetime',
    updated_at  datetime comment 'update datetime'
);

-- 创建合约表
create table if not exists dice_api_contracts
(
    id              bigint unsigned auto_increment primary key comment 'primary key',
    asset_id        varchar(191) comment 'asset id',
    asset_name varchar(191) comment 'asset name',
    org_id          bigint comment 'organization id',
    swagger_version varchar(16) comment 'swagger version',
    client_id       bigint comment 'primary key of table dice_api_client',
    status          varchar(16) comment 'proved:已授权, proving:待审批, disproved:已撤销',
    creator_id      varchar(191) comment 'creator user id',
    updater_id      varchar(191) comment 'updater user id',
    created_at      datetime comment 'created datetime',
    updated_at      datetime comment 'created datetime'
);

-- 创建合约修改记录表
create table if not exists dice_api_contract_records
(
    id bigint unsigned auto_increment primary key comment 'primary key',
    org_id bigint comment 'organization id',
    contract_id bigint comment 'dice_api_contracts primary key',
    action varchar(64) comment 'operation describe',
    creator_id varchar(191) comment 'operation user id',
    created_at datetime comment 'operation datetime'
);

alter table dice_api_assets
    add public  boolean default false comment 'public',
    add cur_version_id bigint   comment 'latest version id',
    add cur_major int(11)       comment 'latest version major',
    add cur_minor int(11)       comment 'latest version minor',
    add cur_patch int(11)       comment 'latest version patch',
    add project_name varchar(191) comment 'project name',
    add app_name varchar(191)   comment 'app name';

-- 为 dice_api_asset_version_specs 添加冗余字段
alter table dice_api_asset_version_instances
    add swagger_version varchar(16) comment 'swagger version',
    add major int(11)               comment 'major',
    add minor int(11)               comment 'minor';

-- 为 dice_api_asset_versions 添加冗余字段
alter table dice_api_asset_version_specs
    add asset_name varchar(191) comment 'asset name';

-- 创建 访问管理 表
alter table dice_api_asset_versions
    add asset_name varchar(191) comment 'asset name';