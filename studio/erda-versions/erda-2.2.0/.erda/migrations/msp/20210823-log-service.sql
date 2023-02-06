CREATE TABLE IF NOT EXISTS sp_log_service_instance
(
    `id`          varchar(128)  NOT NULL PRIMARY KEY COMMENT 'log-instance唯一ID',
    `es_urls`     varchar(1024) NOT NULL COMMENT 'es urls, split by comma',
    `es_config`   varchar(1024) NOT NULL COMMENT 'es connection info, serialized json format',
    `created_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    `updated_at`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time'
) DEFAULT CHARSET=utf8mb4 COMMENT '日志服务实例表';


INSERT INTO tb_tmc_ini (`ini_name`, `ini_desc`, `ini_value`, `create_time`, `update_time`, `is_deleted`)
VALUES ('MK_log-service', '', 'LogAnalyze', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO tb_tmc (`name`, `engine`, `service_type`, `deploy_mode`, `create_time`, `update_time`, `is_deleted`)
VALUES ('日志服务', 'log-service', 'ADDON', 'SAAS', DEFAULT, DEFAULT, DEFAULT);

INSERT INTO tb_tmc_version (`engine`, `version`, `release_id`, `create_time`, `update_time`, `is_deleted`)
VALUES ('log-service', '1.0.0', null, DEFAULT, DEFAULT, DEFAULT);

ALTER TABLE sp_log_deployment ADD `log_type` varchar(20) DEFAULT 'log-analytics' NOT NULL COMMENT '采用的日志服务类型';

ALTER TABLE sp_log_instance ADD `log_type` varchar(20) DEFAULT 'log-analytics' NOT NULL COMMENT '采用的日志服务类型';
