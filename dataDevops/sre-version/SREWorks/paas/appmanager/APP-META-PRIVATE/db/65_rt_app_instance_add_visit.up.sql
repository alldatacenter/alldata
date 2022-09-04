alter table am_rt_app_instance
    add visit tinyint(1) default 0 not null comment '是否可访问';

create index idx_visit
    on am_rt_app_instance (visit);
