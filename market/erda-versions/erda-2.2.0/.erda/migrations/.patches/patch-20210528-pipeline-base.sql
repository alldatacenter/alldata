ALTER TABLE pipeline_extras MODIFY snippets MEDIUMTEXT COMMENT 'snippet 历史';

ALTER TABLE pipeline_tasks MODIFY result MEDIUMTEXT COMMENT 'result';

ALTER TABLE ci_v3_build_caches DEFAULT CHARACTER SET utf8;
