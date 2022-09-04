ALTER TABLE `am_user_profile` DROP INDEX `uk_user_id`;
ALTER TABLE `am_user_profile` ADD UNIQUE INDEX uk_user_namespace_stage (`user_id`,`namespace_id`,`stage_id`) USING BTREE;