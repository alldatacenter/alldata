ALTER TABLE `ta_service_meta`
    ADD COLUMN `category` varchar(64) NULL COMMENT '分类名称',
    ADD KEY `idx_service_code` (`service_code`),
    ADD KEY `idx_category` (`category`);
