ALTER TABLE pipeline_extras MODIFY snippets MEDIUMTEXT COMMENT 'snippet 历史';

ALTER TABLE pipeline_tasks MODIFY result MEDIUMTEXT COMMENT 'result';

ALTER TABLE dice_api_test MODIFY api_info LONGTEXT COMMENT 'API 信息';
