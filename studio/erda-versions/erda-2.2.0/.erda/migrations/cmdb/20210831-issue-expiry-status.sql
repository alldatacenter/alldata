ALTER TABLE dice_issues ADD `expiry_status` VARCHAR(40) NOT NULL DEFAULT '' COMMENT 'issue expiry status';

ALTER TABLE dice_issues ADD INDEX `idx_expiry_status` (`expiry_status`);
