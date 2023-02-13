-- apim 3.19 assets 表增加了当前最新的版本信息
-- 将当前最新的版本信息同步到 assets 表
update dice_api_assets assets
    inner join (
        select id, org_id, asset_id, major, minor, patch
        from dice_api_asset_versions
        group by id, org_id, asset_id, major, minor, patch
        order by org_id, asset_id, major desc, minor desc, patch desc, id
    ) b
    on assets.org_id = b.org_id and assets.asset_id = b.asset_id
set assets.cur_version_id = b.id,
    assets.cur_major      = b.major,
    assets.cur_minor      = b.minor,
    assets.cur_patch      = b.patch
;

-- apim 3.19 assets 表增加 public 字段,
-- 一律设置为 false
update dice_api_assets
set public = false
where public is null
;

-- assets 表增加了 project_name 字段
-- 需要用到 项目表
update dice_api_assets a
    inner join ps_group_projects b
    on a.org_id = b.org_id and a.project_id = b.id
set a.project_name = b.name
;

-- assets 表增加了 app_name 字段
-- 需要用到 应用表
update dice_api_assets a
    inner join ps_projects b
    on a.org_id = b.org_id and a.app_id = b.id
set a.app_name = b.name
;

-- 实例表增加了 org_id 和 版本号信息
-- 从 version 表中同步过来
update dice_api_asset_version_instances a
    inner join dice_api_asset_versions b
    on a.org_id = b.org_id and a.asset_id = b.asset_id and a.version_id = b.id
        -- 3.18 创建实例时, 会记录 version_id, 所以此处用 version_id 关联
        -- 但 3.19 不再记录 version_id 了
set a.org_id          = b.org_id, -- 3.18 表中没 org_id, 生产环境只有一条记录, 所以可以把 org_id 同步过去
    a.swagger_version = b.swagger_version,
    a.major           = b.major,
    a.minor           = b.minor
;

-- spec 表增加了 asset_name
-- 从 asset 表中同步过来
update dice_api_asset_version_specs a
    inner join dice_api_assets b
    on a.org_id = b.org_id and a.asset_id = b.asset_id
set a.asset_name = b.asset_name
;

-- version 表增加了 asset_name
-- 从 asset 表中同步过来
update dice_api_asset_versions a
    inner join dice_api_assets b
    on a.org_id = b.org_id and a.asset_id = b.asset_id
set a.asset_name = b.asset_name
;

-- 实例表增加了 project_id, app_id,
-- 3.18 已有的数据中实例的 project 和 app 于 api asset 关联的是一致的,
-- 所以此处用 api asset 表的项目和应用作为实例表的项目与应用
update dice_api_asset_version_instances a
    inner join dice_api_assets b
    on a.asset_id = b.asset_id
set a.project_id = b.project_id,
    a.app_id     = b.app_id
;