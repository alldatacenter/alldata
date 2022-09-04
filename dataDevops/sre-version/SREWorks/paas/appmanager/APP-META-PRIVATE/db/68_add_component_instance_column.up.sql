alter table am_rt_component_instance
    add watch_kind varchar(16) null comment '监听类型 (KUBERNETES_INFORMER/CRON)' after status;

alter table am_rt_component_instance
    add times bigint default 0 null comment '查询次数 (查询级别升降档匹配使用)' after watch_kind;

create index idx_times
    on am_rt_component_instance (times);

create index idx_watch_kind
    on am_rt_component_instance (watch_kind);

