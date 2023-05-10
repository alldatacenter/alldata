ALTER TABLE dice_labels DROP INDEX `idx_project_name`;
ALTER TABLE dice_labels ADD CONSTRAINT `uk_project_name_type` UNIQUE (`project_id`, `name`, `type`);