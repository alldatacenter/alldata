ALTER TABLE `am_app_package_tag`
    ADD COLUMN `app_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '应用唯一标示';

ALTER TABLE `am_app_package_component_rel`
    ADD COLUMN `app_id` VARCHAR(32) NULL DEFAULT NULL COMMENT '应用唯一标示';