alter table am_workflow_task
    add deploy_app_unit_id varchar(64) default '' null;

alter table am_workflow_task
    add deploy_app_namespace_id varchar(64) default '' null;

alter table am_workflow_task
    add deploy_app_stage_id varchar(64) default '' null;