ALTER TABLE `am_component_package`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT 'VERSION' AFTER `package_options`;