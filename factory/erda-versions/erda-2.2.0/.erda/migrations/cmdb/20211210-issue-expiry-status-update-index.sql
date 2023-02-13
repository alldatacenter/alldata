ALTER TABLE dice_issues ADD INDEX `idx_planfinishedat_deleted_expirystatus` (`plan_finished_at`, `deleted`, `expiry_status`);
