ALTER TABLE `dice_notify_histories` ADD INDEX `idx_sourceid_sourcetype_createdat_status` (`source_id`, `source_type`, `created_at`, `status`);
