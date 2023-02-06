ALTER TABLE `s_instance_info`
ADD INDEX `idx_taskid_cluster_phase_updatedat`
(
    `task_id`,
    `cluster`,
    `phase`,
    `updated_at`
);