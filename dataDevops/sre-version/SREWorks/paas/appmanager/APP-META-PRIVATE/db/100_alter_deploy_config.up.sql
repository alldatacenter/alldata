alter table am_deploy_config
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_deploy_config
    add stage_id varchar(64) default '' null comment 'Stage ID';

create index idx_namespace_stage
    on am_deploy_config (namespace_id, stage_id);

drop index uk_deploy_config on am_deploy_config;

create unique index uk_deploy_config
    on am_deploy_config (api_version, app_id, env_id, type_id, namespace_id, stage_id);

alter table am_deploy_config_history
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_deploy_config_history
    add stage_id varchar(64) default '' null comment 'Stage ID';

create index idx_namespace_stage
    on am_deploy_config_history (namespace_id, stage_id);

