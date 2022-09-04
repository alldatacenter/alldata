alter table am_rt_app_instance_history
    add version varchar(32) not null comment '应用实例版本号';

alter table am_rt_component_instance_history
    add version varchar(32) not null comment '组件实例版本号';

alter table am_rt_trait_instance_history
    add version varchar(32) not null comment 'Trait 实例版本号';
