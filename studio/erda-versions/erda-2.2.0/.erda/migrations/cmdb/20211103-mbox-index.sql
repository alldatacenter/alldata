ALTER TABLE `dice_mboxs` ADD INDEX `idx_deduplicateid_userid_orgid` (`deduplicate_id`, `user_id`, `org_id`);
