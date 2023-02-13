alter table am_app_package_task
    add env_id varchar(16) null comment '环境 ID';

create index idx_env_id
    on am_app_package_task (env_id);

alter table am_component_package_task
    add env_id varchar(16) null comment '环境 ID';

create index idx_env_id
    on am_component_package_task (env_id);

create index idx_app_id
    on am_app_package_task (app_id);

create index idx_app_package_id
    on am_app_package_task (app_package_id);

create index idx_gmt_create
    on am_app_package_task (gmt_create);

create index idx_gmt_modified
    on am_app_package_task (gmt_modified);

create index idx_package_creator
    on am_app_package_task (package_creator);

create index idx_package_version
    on am_app_package_task (package_version);

create index idx_task_status
    on am_app_package_task (task_status);

create index idx_app_id
    on am_component_package_task (app_id);