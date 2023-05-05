alter table ps_v2_deployments add column release_id varchar(255) null;
alter table ps_v2_deployments add column extra text null;
alter table ps_v2_project_runtimes add column cluster_name varchar(255) null;

alter table ps_runtime_instances add column `stage` varchar(100) default null after `status`;

update `ps_v2_project_runtimes` set runtime_status='Unhealthy' where runtime_status='Running';
update `ps_v2_project_runtimes` set runtime_status='Healthy' where runtime_status='Ready';

update `ps_runtime_services` set status='Unhealthy' where status in ('Progressing', 'Running');
update `ps_runtime_services` set status='Healthy' where status='Ready';

update `ps_runtime_instances` set status='Starting' where status='Running';
