ALTER TABLE dice_api_contract_records
    ADD COLUMN sla_name VARCHAR(191) NOT NULL DEFAULT '' COMMENT 'sla name';