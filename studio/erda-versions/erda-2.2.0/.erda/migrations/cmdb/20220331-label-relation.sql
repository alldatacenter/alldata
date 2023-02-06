ALTER TABLE dice_label_relations
    MODIFY `ref_id` varchar(255) NOT NULL COMMENT '标签关联目标 id, eg: issue_id'