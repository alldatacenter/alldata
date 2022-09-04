alter table am_deploy_app
    add app_source varchar(32) null comment '应用来源';

create index idx_app_source
    on am_deploy_app (app_source);