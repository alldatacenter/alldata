alter table am_rt_app_instance
    modify namespace_id varchar(64) null comment 'Namespace ID';

alter table am_rt_component_instance
    modify namespace_id varchar(64) null comment 'Namespace ID';

alter table am_addon_instance
    modify namespace_id varchar(64) null comment '该附加组件部署到的 Namespace 标识';

alter table am_addon_instance_task
    modify namespace_id varchar(64) null comment '命名空间 ID';

alter table am_app_instance
    modify namespace_id varchar(64) not null comment 'Namespace ID';

alter table am_deploy_app
    modify namespace_id varchar(64) default '' not null comment 'Namespace ID';

alter table am_deploy_component
    modify namespace_id varchar(64) null comment '部署目标 Namespace ID';

alter table am_env
    modify namespace_id varchar(64) null comment '所属 Namespace ID';

alter table am_namespace
    modify namespace_id varchar(64) null comment 'Namespace ID';

alter table am_user_profile
    modify namespace_id varchar(64) null comment '部署目标 Namespace ID';

