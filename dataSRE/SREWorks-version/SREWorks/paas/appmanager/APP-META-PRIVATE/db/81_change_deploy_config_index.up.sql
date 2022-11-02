alter table am_deploy_config
    drop key uk_deploy_config;

alter table am_deploy_config
    add constraint uk_deploy_config
        unique (api_version, app_id(64), env_id(64), type_id(64));

alter table am_deploy_config
    add constraint idx_deploy_config
        unique (api_version, app_id(64), type_id(64));

drop index idx_deploy_config_history on am_deploy_config_history;

create index idx_deploy_config_history_1
    on am_deploy_config_history (api_version, app_id(64), env_id(64), type_id(64));

create index idx_deploy_config_history_2
    on am_deploy_config_history (api_version, app_id(64), type_id(64));
