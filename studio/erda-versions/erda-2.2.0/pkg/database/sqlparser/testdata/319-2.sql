-- apim 3.19 models changes

alter table dice_api_asset_version_instances
    add project_id bigint(20),
    add app_id     bigint(20);