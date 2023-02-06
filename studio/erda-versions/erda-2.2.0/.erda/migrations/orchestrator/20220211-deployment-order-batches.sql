ALTER TABLE `erda_deployment_order`
    CHANGE `status` `status_detail` text NOT NULL COMMENT 'application status detail';

ALTER TABLE `erda_deployment_order`
    ADD COLUMN `batch_size` bigint(20)NOT NULL DEFAULT 0  COMMENT 'deployment order batch size',
    ADD COLUMN `current_batch` bigint(20) NOT NULL DEFAULT 0  COMMENT 'deployment order current batch',
    ADD COLUMN `status` text NOT NULL COMMENT 'deployment order current batch';
