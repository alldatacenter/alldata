alter table am_app_addon
    add namespace_id varchar(64) default '' null comment 'Namespace ID';

alter table am_app_addon
    add stage_id varchar(64) default '' null comment 'Stage ID';

drop index idx_app_addon_type on am_app_addon;

create index idx_app_addon_type
    on am_app_addon (app_id, addon_type, namespace_id, stage_id);

drop index uk_app_addon on am_app_addon;

create unique index uk_app_addon
    on am_app_addon (app_id, name, namespace_id, stage_id);