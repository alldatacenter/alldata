alter table am_deploy_config_history
    add modifier varchar(64) default '' not null comment '修改者';

alter table am_deploy_config_history
    add deleted tinyint(1) default '0' not null comment '是否被删除';