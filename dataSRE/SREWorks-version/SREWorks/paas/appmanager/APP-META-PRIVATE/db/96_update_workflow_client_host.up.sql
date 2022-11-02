alter table am_workflow_instance
    add client_host varchar(64) default '' not null comment '当前工作 IP';

create index index_client_host
    on am_workflow_instance (client_host);

alter table am_workflow_task
    drop column client_hostname;
