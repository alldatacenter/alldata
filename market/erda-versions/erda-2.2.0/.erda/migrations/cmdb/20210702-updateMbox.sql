ALTER TABLE dice_mboxs ADD COLUMN `deduplicate_id` varchar(255) NOT NULL DEFAULT "" COMMENT 'deduplicate id';
ALTER TABLE dice_mboxs ADD COLUMN `unread_count` bigint(20) NOT NULL DEFAULT 0 COMMENT 'unread count';
