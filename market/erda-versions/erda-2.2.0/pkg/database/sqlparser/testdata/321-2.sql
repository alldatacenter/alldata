-- 3.21 自动化测试接入 API 集市

ALTER TABLE dice_api_asset_versions
    ADD source       VARCHAR(16)  NOT NULL COMMENT '该版本文档来源',
    ADD app_id       BIGINT       NOT NULL COMMENT '应用 id',
    ADD branch       VARCHAR(191) NOT NULL COMMENT '分支名',
    Add service_name VARCHAR(191) NOT NULL COMMENT '服务名';


-- 3.21 API 设计中心
CREATE TABLE IF NOT EXISTS dice_api_doc_lock
(
    id             BIGINT AUTO_INCREMENT COMMENT 'primary key' PRIMARY KEY,
    created_at     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    session_id     CHAR(36)                             NOT NULL COMMENT '会话标识',
    is_locked      TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '会话所有者是否持有文档锁',
    expired_at     DATETIME                             NOT NULL COMMENT '会话过期时间',
    application_id BIGINT                               NOT NULL COMMENT '应用 id',
    branch_name    VARCHAR(191)                         NOT NULL COMMENT '分支名',
    doc_name       VARCHAR(191)                         NOT NULL COMMENT '文档名, 也即服务名',
    creator_id     VARCHAR(191)                         NOT NULL COMMENT '创建者 id',
    updater_id     VARCHAR(191)                         NOT NULL COMMENT '更新者 id',
    CONSTRAINT uk_doc
        UNIQUE (application_id, branch_name, doc_name)
) CHARSET = utf8mb4 COMMENT 'API 设计中心文档锁表';


CREATE TABLE IF NOT EXISTS dice_api_doc_tmp_content
(
    id             BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'primary key' PRIMARY KEY,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'created time',
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP comment 'updated time',
    application_id BIGINT                             NOT NULL COMMENT '应用 id',
    branch_name    VARCHAR(191)                       NOT NULL COMMENT '分支名',
    doc_name       VARCHAR(64)                        NOT NULL COMMENT '文档名',
    content        LONGTEXT                           NOT NULL COMMENT 'API doc text',
    creator_id     VARCHAR(191)                       NOT NULL COMMENT 'creator id',
    updater_id     VARCHAR(191)                       NOT NULL COMMENT 'updater id',
    CONSTRAINT uk_inode
        UNIQUE (application_id, branch_name, doc_name)
) CHARSET = utf8mb4 COMMENT 'API 设计中心文档临时存储表';


-- 存储 openapi 3
CREATE TABLE IF NOT EXISTS dice_api_oas3_index
(
    id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'primary key' PRIMARY KEY,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    asset_id     VARCHAR(191) NOT NULL COMMENT 'asset id',
    asset_name   VARCHAR(191) NOT NULL COMMENT 'asset name',
    info_version VARCHAR(191) NOT NULL COMMENT '.info.version value, 也即 swaggerVersion',
    version_id   BIGINT       NOT NULL COMMENT 'asset version primary key',
    path         VARCHAR(191) NOT NULL COMMENT '.paths.{path}',
    method       VARCHAR(16)  NOT NULL COMMENT '.paths.{path}.{method}',
    operation_id VARCHAR(191) NOT NULL COMMENT '.paths.{path}.{method}.operationId',
    description  TEXT         NOT NULL COMMENT '.path.{path}.{method}.description',

    CONSTRAINT uk_path_method
        UNIQUE (version_id, path, method) COMMENT '同一文档下, path + method 确定一个接口'
) CHARSET = utf8mb4 COMMENT 'API 集市 operation 搜索索引表';

CREATE TABLE IF NOT EXISTS dice_api_oas3_fragment
(
    id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'primary key' PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    index_id   BIGINT   NOT NULL COMMENT 'dice_api_oas3_index primary key',
    version_id BIGINT   NOT NULL COMMENT 'asset version primary key',
    operation  TEXT     NOT NULL COMMENT '.paths.{path}.{method}.parameters, 序列化了的 parameters JSON 片段'
) CHARSET = utf8mb4 COMMENT 'API 集市 oas3 片段表';
