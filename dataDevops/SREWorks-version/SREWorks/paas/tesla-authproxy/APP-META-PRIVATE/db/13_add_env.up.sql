ALTER TABLE `ta_user`
    DROP KEY `idx_user_id`,
    ADD UNIQUE KEY `uk_user_id` (`user_id`);
