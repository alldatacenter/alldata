drop index idx_app_source on am_deploy_app;

alter table am_deploy_app drop column app_source;