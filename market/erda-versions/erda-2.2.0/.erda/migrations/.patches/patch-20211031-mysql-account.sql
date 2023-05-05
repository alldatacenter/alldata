UPDATE tb_addon_instance SET kms_key = '' WHERE kms_key IS NULL;

ALTER TABLE tb_addon_instance MODIFY COLUMN `kms_key` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'kms key id';
