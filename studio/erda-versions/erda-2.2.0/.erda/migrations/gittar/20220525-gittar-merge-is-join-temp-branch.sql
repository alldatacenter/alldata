ALTER TABLE dice_repo_merge_requests ADD is_join_temp_branch tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否加入临时分支';
ALTER TABLE dice_repo_merge_requests MODIFY join_temp_branch_status varchar(255) COMMENT '加入临时分支的状态';
