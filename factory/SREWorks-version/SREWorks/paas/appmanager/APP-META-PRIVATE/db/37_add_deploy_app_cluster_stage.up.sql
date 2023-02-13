alter table am_deploy_app
    add namespace_id varchar(32) default "" not null comment 'Namespace ID';

create index idx_namespace_id
    on am_deploy_app (namespace_id);

alter table am_deploy_app
    add stage_id varchar(32) default "" not null comment 'Stage ID';

create index idx_stage_id
    on am_deploy_app (stage_id);
