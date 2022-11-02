alter table am_deploy_app
    add cluster_id varchar(32) default "" not null comment '集群 ID';

create index idx_cluster_id
    on am_deploy_app (cluster_id);