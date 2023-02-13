/*
为dice_api-asset_version_specs.spec 添加Fulltext索引
*/
CREATE FULLTEXT INDEX ft_specs
    ON dice_api_asset_version_specs (spec);

/*
为dice_api_asset_versions增加字段swagger_version
*/
ALTER TABLE dice_api_asset_versions
    ADD swagger_version varchar(16) not null comment '';