drop index uk_deploy_config on am_deploy_config;
alter table am_deploy_config
    add constraint uk_deploy_config
        unique (api_version, app_id, env_id, type_id);
