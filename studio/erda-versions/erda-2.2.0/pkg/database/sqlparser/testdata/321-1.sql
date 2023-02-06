-- 3.21 table comments

alter table dice_api_access
    comment 'API 集市资源访问管理表';

alter table dice_api_asset_version_instances
    comment '特定版本的 API 集市资源绑定的实例表';

alter table dice_api_asset_version_specs
    comment '特定版本的 API 集市资源的 swagger specification 内容';

alter table dice_api_asset_versions
    comment 'API 集市资源的版本列表';

alter table dice_api_assets
    comment 'API 集市资源表';

alter table dice_api_clients
    comment 'API 集市资源访问管理表';

alter table dice_api_contract_records
    comment 'API 集市资源访问管理合约操作记录表';

alter table dice_api_contracts
    comment 'API 集市资源访问管理合约表';

alter table dice_api_sla_limits
    comment 'API 集市访问管理 SLA 限制条件表';

alter table dice_api_slas
    comment 'API 集市访问管理 Service Level Agreements 表';

-- 3.21 column comments

alter table dice_api_asset_version_instances
    modify id bigint unsigned auto_increment comment 'primary key',
    modify name varchar(191) comment '实例名',
    modify asset_id varchar(191) comment 'API 集市资源 id',
    modify version_id bigint comment 'dice_api_asset_versions primary key',
    modify type varchar(32) comment '实例类型',
    modify runtime_id bigint comment 'runtime id',
    modify service_name varchar(191) comment '服务名称',
    modify endpoint_id varchar(191) comment '流量入口 endpoint id',
    modify url varchar(1024) comment '实例 url',
    modify creator_id varchar(191) comment '创建者 user id',
    modify updater_id varchar(191) comment '更新者 user id',
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    modify project_id bigint comment 'project id',
    modify app_id bigint comment 'application id'
;


alter table dice_api_asset_version_specs
    modify id bigint unsigned auto_increment comment 'primary key',
    modify org_id bigint comment 'organization id',
    modify asset_id varchar(191) comment 'API 集市资源 id',
    modify version_id bigint comment 'dice_api_asset_versions primary key',
    modify spec_protocol varchar(32) comment 'swagger protocol',
    modify spec longtext comment 'swagger text',
    modify creator_id varchar(191) comment 'creator user id',
    modify updater_id varchar(191) comment 'updater user id',
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_asset_versions
    modify id bigint unsigned auto_increment comment 'primary key',
    modify org_id bigint comment 'organization id',
    modify asset_id varchar(191) comment 'API 集市资源 id',
    modify major int comment 'version major number',
    modify minor int comment 'version minor number',
    modify patch int comment 'version patch number',
    modify `desc` varchar(1024) comment 'description',
    modify spec_protocol varchar(32) comment 'swagger protocol',
    modify creator_id varchar(191) comment 'creator user id',
    modify updater_id varchar(191) comment 'updater user id',
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    modify swagger_version varchar(16) comment '用户自定义的版本号, 相当于一个 tag'
;

alter table dice_api_assets
    modify id bigint unsigned auto_increment comment 'primary key',
    modify asset_id varchar(191) comment 'API 集市资源 id',
    modify asset_name varchar(191) comment '集市名称',
    modify `desc` varchar(1024) comment '描述信息',
    modify logo varchar(1024) comment 'logo 地址',
    modify org_id bigint comment 'organization id',
    modify project_id bigint comment '项目 id',
    modify app_id bigint comment '应用 id',
    modify creator_id varchar(191) comment 'creator user id',
    modify updater_id varchar(191) comment 'updater user id',
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_clients
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_contract_records
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间'
;

alter table dice_api_contract_records
    add updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_contracts
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_sla_limits
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

alter table dice_api_slas
    modify created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP comment '创建时间',
    modify updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP comment '更新时间'
;

