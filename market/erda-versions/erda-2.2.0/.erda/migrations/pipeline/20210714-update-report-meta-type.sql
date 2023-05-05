-- update dice_pipeline_reports meta filed to MEDIUMTEXT, because meta filed will save task message
-- TEXT type is not enough
ALTER TABLE dice_pipeline_reports MODIFY meta MEDIUMTEXT NOT NULL COMMENT '报告元数据';