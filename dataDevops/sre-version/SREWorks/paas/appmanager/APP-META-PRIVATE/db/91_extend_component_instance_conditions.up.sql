alter table am_rt_component_instance
    modify conditions longtext null comment '当前状态详情 (Yaml Array)';

alter table am_rt_component_instance_history
    modify conditions longtext null comment '当前状态详情 (Yaml Array)';
