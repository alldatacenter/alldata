alter table am_component_package
    modify component_name varchar(64) null comment '组件类型下的唯一组件标识';

alter table am_component_package_task
    modify component_name varchar(64) null comment '组件类型下的唯一组件标识';

alter table am_rt_component_instance
    modify component_name varchar(64) not null comment '组件类型下的唯一组件标识';

