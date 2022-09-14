drop index uk_app_instance_location on am_rt_app_instance;

alter table am_rt_app_instance
    add stages text null comment '部署阶段列表';

alter table am_rt_app_instance drop column namespace_id;

alter table am_rt_app_instance drop column stage_id;

create index idx_app_instance_location
    on am_rt_app_instance (app_id, cluster_id);
