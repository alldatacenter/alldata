alter table am_dynamic_script
    modify name varchar(64) not null comment '标识名称';

alter table am_dynamic_script_history
    modify name varchar(64) not null comment '标识名称';