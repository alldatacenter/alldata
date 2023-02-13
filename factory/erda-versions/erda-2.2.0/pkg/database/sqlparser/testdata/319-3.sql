-- apim 3.19 models changes

alter table dice_api_asset_version_instances
    add org_id bigint(20) comment 'organization id',
    add workspace varchar(16) comment 'env';

update dice_api_asset_version_instances a
    inner join dice_api_asset_versions b on a.asset_id = b.asset_id and a.swagger_version = b.swagger_version
set a.org_id = b.org_id;