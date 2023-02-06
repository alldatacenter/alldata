ALTER TABLE dice_issue_relation DROP INDEX issue_related;

ALTER TABLE dice_issue_relation ADD `type` VARCHAR(40) NOT NULL DEFAULT '' COMMENT 'issue relation type';

UPDATE dice_issue_relation SET `type` = 'connection' WHERE `type` = '';
