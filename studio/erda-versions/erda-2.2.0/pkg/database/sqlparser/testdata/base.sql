create table dice_api_access
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    asset_id varchar(191) null comment 'asset id',
    asset_name varchar(191) null comment 'asset name',
    org_id bigint null comment 'organization id',
    swagger_version varchar(16) null comment 'swagger version',
    major int null comment 'version major number',
    minor int null comment 'version minor number',
    project_id bigint null comment 'project id',
    app_id bigint null comment 'application id',
    workspace varchar(32) null comment 'DEV, TEST, STAGING, PROD',
    endpoint_id varchar(32) null comment 'gateway endpoint id',
    authentication varchar(32) null comment 'api-key, parameter-sign, auth2',
    authorization varchar(32) null comment 'auto, manual',
    addon_instance_id varchar(128) null comment 'addon instance id',
    bind_domain varchar(256) null comment 'bind domains',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime null comment 'created datetime',
    updated_at datetime null comment 'last updated datetime',
    project_name varchar(191) null comment 'project name',
    app_name varchar(191) null comment 'app name',
    default_sla_id bigint null comment 'default SLA id'
)
    comment 'API 集市资源访问管理表';

create table dice_api_asset_version_instances
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    name varchar(191) null comment '实例名',
    asset_id varchar(191) null comment 'API 集市资源 id',
    version_id bigint null comment 'dice_api_asset_versions primary key',
    type varchar(32) null comment '实例类型',
    runtime_id bigint null comment 'runtime id',
    service_name varchar(191) null comment '服务名称',
    endpoint_id varchar(191) null comment '流量入口 endpoint id',
    url varchar(1024) null comment '实例 url',
    creator_id varchar(191) null comment '创建者 user id',
    updater_id varchar(191) null comment '更新者 user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    swagger_version varchar(16) null comment 'swagger version',
    major int null comment 'major',
    minor int null comment 'minor',
    project_id bigint null comment 'project id',
    app_id bigint null comment 'application id',
    org_id bigint null comment 'organization id',
    workspace varchar(16) null comment 'env'
)
    comment '特定版本的 API 集市资源绑定的实例表';

create table dice_api_asset_version_specs
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    org_id bigint null comment 'organization id',
    asset_id varchar(191) null comment 'API 集市资源 id',
    version_id bigint null comment 'dice_api_asset_versions primary key',
    spec_protocol varchar(32) null comment 'swagger protocol',
    spec longtext null comment 'swagger text',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    asset_name varchar(191) null comment 'asset name'
)
    comment '特定版本的 API 集市资源的 swagger specification 内容';

create fulltext index ft_specs
    on dice_api_asset_version_specs (spec);

create table dice_api_asset_versions
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    org_id bigint null comment 'organization id',
    asset_id varchar(191) null comment 'API 集市资源 id',
    major int null comment 'version major number',
    minor int null comment 'version minor number',
    patch int null comment 'version patch number',
    `desc` varchar(1024) null comment 'description',
    spec_protocol varchar(32) null comment 'swagger protocol',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    swagger_version varchar(16) null comment '用户自定义的版本号, 相当于一个 tag',
    asset_name varchar(191) null comment 'asset name',
    deprecated tinyint(1) default 0 null comment 'is the asset version deprecated',
    source varchar(16) not null comment '该版本文档来源',
    app_id bigint not null comment '应用 id',
    branch varchar(191) not null comment '分支名',
    service_name varchar(191) not null comment '服务名'
)
    comment 'API 集市资源的版本列表';

create table dice_api_assets
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    asset_id varchar(191) null comment 'API 集市资源 id',
    asset_name varchar(191) null comment '集市名称',
    `desc` varchar(1024) null comment '描述信息',
    logo varchar(1024) null comment 'logo 地址',
    org_id bigint null comment 'organization id',
    project_id bigint null comment '项目 id',
    app_id bigint null comment '应用 id',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    public tinyint(1) default 0 null comment 'public',
    cur_version_id bigint null comment 'latest version id',
    cur_major int null comment 'latest version major',
    cur_minor int null comment 'latest version minor',
    cur_patch int null comment 'latest version patch',
    project_name varchar(191) null comment 'project name',
    app_name varchar(191) null comment 'app name'
)
    comment 'API 集市资源表';

create table dice_api_clients
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    org_id bigint null comment 'organization id',
    name varchar(64) null comment 'client name',
    `desc` varchar(1024) null comment 'describe',
    client_id varchar(32) null comment 'client id',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    alias_name varchar(64) null comment 'alias name',
    display_name varchar(191) null comment 'client display name'
)
    comment 'API 集市资源访问管理表';

create table dice_api_contract_records
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    org_id bigint null comment 'organization id',
    contract_id bigint null comment 'dice_api_contracts primary key',
    action varchar(64) null comment 'operation describe',
    creator_id varchar(191) null comment 'operation user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment 'API 集市资源访问管理合约操作记录表';

create table dice_api_contracts
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    asset_id varchar(191) null comment 'asset id',
    asset_name varchar(191) null comment 'asset name',
    org_id bigint null comment 'organization id',
    swagger_version varchar(16) null comment 'swagger version',
    client_id bigint null comment 'primary key of table dice_api_client',
    status varchar(16) null comment 'proved:已授权, proving:待审批, disproved:已撤销',
    creator_id varchar(191) null comment 'creator user id',
    updater_id varchar(191) null comment 'updater user id',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    cur_sla_id bigint null comment 'contract current SLA id',
    request_sla_id bigint null comment 'contract request SLA',
    sla_committed_at datetime null comment 'current SLA committed time'
)
    comment 'API 集市资源访问管理合约表';

create table dice_api_doc_lock
(
    id bigint auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    session_id char(36) not null comment '会话标识',
    is_locked tinyint(1) default 0 not null comment '会话所有者是否持有文档锁',
    expired_at datetime not null comment '会话过期时间',
    application_id bigint not null comment '应用 id',
    branch_name varchar(191) not null comment '分支名',
    doc_name varchar(191) not null comment '文档名, 也即服务名',
    creator_id varchar(191) not null comment '创建者 id',
    updater_id varchar(191) not null comment '更新者 id',
    constraint uk_doc
        unique (application_id, branch_name, doc_name)
)
    comment 'API 设计中心文档锁表';

create table dice_api_doc_tmp_content
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment 'created time',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    application_id bigint not null comment '应用 id',
    branch_name varchar(191) not null comment '分支名',
    doc_name varchar(64) not null comment '文档名',
    content longtext not null comment 'API doc text',
    creator_id varchar(191) not null comment 'creator id',
    updater_id varchar(191) not null comment 'updater id',
    constraint uk_inode
        unique (application_id, branch_name, doc_name)
)
    comment 'API 设计中心文档临时存储表';

create table dice_api_oas3_fragment
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment 'created time',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    index_id bigint not null comment 'dice_api_oas3_index primary key',
    version_id bigint not null comment 'asset version primary key',
    operation text not null comment '.paths.{path}.{method}.parameters, 序列化了的 parameters JSON 片段'
)
    comment 'API 集市 oas3 片段表';

create table dice_api_oas3_index
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment 'created time',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'updated time',
    asset_id varchar(191) not null comment 'asset id',
    asset_name varchar(191) not null comment 'asset name',
    info_version varchar(191) not null comment '.info.version value, 也即 swaggerVersion',
    version_id bigint not null comment 'asset version primary key',
    path varchar(191) not null comment '.paths.{path}',
    method varchar(16) not null comment '.paths.{path}.{method}',
    operation_id varchar(191) not null comment '.paths.{path}.{method}.operationId',
    description text not null comment '.path.{path}.{method}.description',
    constraint uk_path_method
        unique (version_id, path, method) comment '同一文档下, path + method 确定一个接口'
)
    comment 'API 集市 operation 搜索索引表';

create table dice_api_sla_limits
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    creator_id varchar(191) null comment 'creator id',
    updater_id varchar(191) null comment 'creator id',
    sla_id bigint null comment 'SLA model id',
    `limit` bigint null comment 'request limit',
    unit varchar(16) null comment 's: second, m: minute, h: hour, d: day'
)
    comment 'API 集市访问管理 SLA 限制条件表';

create table dice_api_slas
(
    id bigint unsigned auto_increment comment 'primary key'
        primary key,
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    creator_id varchar(191) null comment 'creator id',
    updater_id varchar(191) null comment 'creator id',
    name varchar(191) null comment 'SLA name',
    `desc` varchar(1024) null comment 'description',
    approval varchar(16) null comment 'auto, manual',
    access_id bigint null comment 'access id'
)
    comment 'API 集市访问管理 Service Level Agreements 表';

