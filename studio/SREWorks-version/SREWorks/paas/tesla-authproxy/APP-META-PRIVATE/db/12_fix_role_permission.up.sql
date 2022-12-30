ALTER TABLE `ta_role_permission_rel`
    DROP KEY `idx_location`,
    ADD KEY `idx_location` (`tenant_id`,`role_id`),
    ADD KEY `idx_service_code` (`service_code`);

