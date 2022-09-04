alter table am_deploy_app
    add config_sha256 varchar(256) default "" not null comment 'Configuration SHA256';
