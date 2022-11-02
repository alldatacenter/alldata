alter table am_rt_app_instance
    add upgrade tinyint(1) default 0 null comment '是否可升级';

create index idx_upgrade
    on am_rt_app_instance (upgrade);