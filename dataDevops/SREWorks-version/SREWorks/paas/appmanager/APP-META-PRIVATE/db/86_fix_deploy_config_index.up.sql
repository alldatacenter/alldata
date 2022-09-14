drop index idx_deploy_config on am_deploy_config;

create index idx_deploy_config
    on am_deploy_config (api_version, app_id, type_id(64));
