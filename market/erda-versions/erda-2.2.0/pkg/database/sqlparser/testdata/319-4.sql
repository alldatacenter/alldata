-- apim 3.19 models changes

alter table dice_api_access
    add project_name varchar(191) comment 'project name',
    add app_name varchar(191) comment 'app name';

alter table dice_api_clients
    add alias_name varchar(64) comment 'alias name';