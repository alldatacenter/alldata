truncate table am_rt_app_instance;

drop index idx_app_instance_location on am_rt_app_instance;

alter table am_rt_app_instance drop column stages;

alter table am_rt_app_instance
    add namespace_id varchar(32) null comment 'Namespace ID';

alter table am_rt_app_instance
    add stage_id varchar(32) null comment 'Stage ID';

create unique index uk_app_instance_location
    on am_rt_app_instance (app_id, cluster_id, namespace_id, stage_id);
