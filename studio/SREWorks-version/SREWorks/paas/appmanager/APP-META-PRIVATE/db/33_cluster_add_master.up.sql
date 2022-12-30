ALTER TABLE `am_cluster`
    ADD COLUMN `master_flag` tinyint(1) DEFAULT 0 COMMENT 'Master 标记';

alter table am_deploy_component
    add cluster_id varchar(32) default "" not null comment '集群 ID';

create index idx_cluster_id
    on am_deploy_component (cluster_id);

alter table am_deploy_component
    add stage_id varchar(32) default "" not null comment 'Stage ID';

create index idx_stage_id
    on am_deploy_component (stage_id);