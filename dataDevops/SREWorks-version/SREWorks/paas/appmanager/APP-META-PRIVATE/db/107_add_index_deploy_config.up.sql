alter table am_deploy_config
    add product_id varchar(32) default '' null;

alter table am_deploy_config
    add release_id varchar(32) default '' null;

alter table am_deploy_config_history
    add product_id varchar(32) default '' null;

alter table am_deploy_config_history
    add release_id varchar(32) default '' null;
