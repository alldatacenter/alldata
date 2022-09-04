alter table am_deploy_config
    add inherit tinyint(1) default '0' not null comment '是否继承父级配置';

alter table am_deploy_config_history
    add inherit tinyint(1) default '0' not null comment '是否继承父级配置';

alter table am_deploy_config
    add constraint idx_inherit
        unique (inherit);

alter table am_deploy_config_history
    add constraint idx_inherit
        unique (inherit);