-- MIGRATION_BASE

CREATE TABLE `openapi_oauth2_token_clients`
(
    `id`         varchar(191)  NOT NULL,
    `secret`     varchar(191)  NOT NULL,
    `domain`     varchar(4096) NOT NULL DEFAULT '',
    `created_at` datetime      NOT NULL,
    `updated_at` datetime      NOT NULL,
    PRIMARY KEY (`id`),
    KEY          `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='openapi oauth2 客户端表';


INSERT INTO `openapi_oauth2_token_clients` (`id`, `secret`, `domain`, `created_at`, `updated_at`)
VALUES ('action-runner', 'devops/action-runner', '', '2021-05-28 11:55:00', '2021-05-28 11:55:00'),
       ('elf', 'ai/elf', '', '2020-09-03 00:00:00', '2020-09-03 00:00:00'),
       ('fdp', 'fdp/agent', '', '2020-03-20 00:00:00', '2020-03-20 00:00:00'),
       ('orchestrator', 'devops/orchestrator', '', '2020-08-03 00:00:00', '2020-08-03 00:00:00'),
       ('pipeline', 'devops/pipeline', '', '2020-03-20 00:00:00', '2020-03-20 00:00:00');

CREATE TABLE `openapi_oauth2_tokens`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT,
    `code`       varchar(191)           DEFAULT NULL,
    `access`     varchar(4096) NOT NULL DEFAULT '',
    `refresh`    varchar(4096) NOT NULL DEFAULT '',
    `data`       text          NOT NULL,
    `created_at` datetime      NOT NULL,
    `expired_at` datetime               DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY          `idx_expired_at` (`expired_at`),
    KEY          `idx_created_at` (`created_at`),
    KEY          `idx_code` (`code`),
    FULLTEXT KEY `idx_access` (`access`),
    FULLTEXT KEY `idx_refresh` (`refresh`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COMMENT='openapi oauth2 token 表';

