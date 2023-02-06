ALTER TABLE dice_audit_history ADD COLUMN `fdp_project_id` varchar(128) DEFAULT "" COMMENT 'fdp project id';

UPDATE dice_audit SET `fdp_project_id` = "" WHERE `fdp_project_id` IS NULL;
ALTER TABLE dice_audit MODIFY COLUMN `fdp_project_id` varchar(128) DEFAULT "" COMMENT 'fdp project id';
