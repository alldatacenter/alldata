alter table `pipeline_definition_extra` change column `soft_deleted_at` `soft_deleted_at`  bigint(20) NOT NULL DEFAULT 0 COMMENT '软删除';
