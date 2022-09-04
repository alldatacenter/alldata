alter table am_component_package_task
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_component_package_task
    add stage_id varchar(64) default '' null comment 'Stage ID';

create index idx_namespace_stage
    on am_component_package_task (namespace_id, stage_id);

drop table if exists am_micro_service_host;
drop table if exists am_micro_service_meta;
drop table if exists am_ms_port_resource;
drop table if exists am_app_instance;

alter table am_k8s_micro_service_meta
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_k8s_micro_service_meta
    add stage_id varchar(64) default '' null comment 'Stage ID';

drop index uk_app_micro_service on am_k8s_micro_service_meta;

create unique index uk_app_micro_service
    on am_k8s_micro_service_meta (app_id, micro_service_id, namespace_id, stage_id);

alter table am_helm_meta
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_helm_meta
    add stage_id varchar(64) default '' null comment 'Stage ID';

drop index uk_helm_meta on am_helm_meta;

create unique index uk_helm_meta
    on am_helm_meta (app_id, helm_package_id, namespace_id, stage_id);
