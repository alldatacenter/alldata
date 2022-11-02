alter table am_dynamic_script
    add kind varchar(16) not null comment '类型';

alter table am_dynamic_script drop key uk_name;

alter table am_dynamic_script
    add constraint uk_kind_name
        unique (kind, name);

alter table am_dynamic_script_history
    add kind varchar(16) not null after id;

alter table am_dynamic_script_history drop key uk_name;

alter table am_dynamic_script_history
    add constraint idx_kind_name
        unique (kind, name);

drop index idx_kind_name on am_dynamic_script_history;

create index idx_kind_name
    on am_dynamic_script_history (kind, name);

