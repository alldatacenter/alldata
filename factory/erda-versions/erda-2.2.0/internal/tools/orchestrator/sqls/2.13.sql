alter table ps_v2_deployments add column outdated tinyint(1) null default '0';
alter table ps_v2_deployments add column finished_at datetime null;

-- mark outdated if runtime already deleted
update ps_v2_deployments set outdated = true where runtime_id not in (select id from ps_v2_project_runtimes) and outdated = false;

-- pad finished_at as old updated_at
update ps_v2_deployments set finished_at = updated_at where status in ('OK', 'FAILED', 'CANCELED') and finished_at is null;

-- reset reference
update ps_releases set reference = 0;
-- set reference count from deployment table
update ps_releases a, (select release_id, count(*) as count from ps_v2_deployments where release_id != '' and outdated = false group by release_id) b set a.`reference`=b.count where a.`release_id`=b.`release_id`;

-- add errors column
alter table ps_runtime_services add column errors text null;

-- add application_id/workspace/name to replace old column project_id/env/git_branch
alter table ps_v2_project_runtimes add column application_id bigint not null;
alter table ps_v2_project_runtimes add column workspace varchar(255) not null;
alter table ps_v2_project_runtimes add column name varchar(255) not null;
-- migrate data from old column
update ps_v2_project_runtimes set application_id = project_id, workspace = env, name = git_branch;
-- add unique index for these three column
alter table ps_v2_project_runtimes add unique index idx_unique_app_id_name (application_id, workspace, name);
-- drop old unnecessary index
alter table ps_v2_project_runtimes drop index idx_unique_env_branch;
