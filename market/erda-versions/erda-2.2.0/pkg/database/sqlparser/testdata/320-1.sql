-- 3.20 模型变更

-- SLA 相关模型

-- SLA 表
CREATE TABLE IF NOT EXISTS dice_api_slas
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    created_at DATETIME COMMENT 'create time',
    updated_at DATETIME COMMENT 'update time',
    creator_id VARCHAR(191) COMMENT 'creator id',
    updater_id VARCHAR(191) COMMENT 'creator id',

    name       VARCHAR(191) COMMENT 'SLA name',
    `desc`     VARCHAR(1024) COMMENT 'description',
    approval   VARCHAR(16) COMMENT 'auto, manual',
    access_id  bigint COMMENT 'access id'
);

-- SLA Limit 表
CREATE TABLE IF NOT EXISTS dice_api_sla_limits
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'primary key',
    created_at DATETIME COMMENT 'create time',
    updated_at DATETIME COMMENT 'update time',
    creator_id VARCHAR(191) COMMENT 'creator id',
    updater_id VARCHAR(191) COMMENT 'creator id',

    sla_id     BIGINT COMMENT 'SLA model id',
    `limit`    BIGINT COMMENT 'request limit',
    unit       VARCHAR(16) COMMENT 's: second, m: minute, h: hour, d: day'
);

-- 其他表修改
ALTER TABLE dice_api_access
    ADD default_sla_id BIGINT COMMENT 'default SLA id';

ALTER TABLE dice_api_contracts
    ADD cur_sla_id     BIGINT COMMENT 'contract current SLA id',
    ADD request_sla_id BIGINT COMMENT 'contract request SLA',
    ADD sla_committed_at DATETIME COMMENT 'current SLA committed time';

ALTER TABLE dice_api_asset_versions
    ADD deprecated BOOL DEFAULT FALSE COMMENT 'is the asset version deprecated';

ALTER TABLE dice_api_clients
    ADD display_name VARCHAR(191) COMMENT 'client display name';

UPDATE dice_api_clients
    SET display_name = name;

UPDATE dice_api_access
    SET authentication = 'key-auth'
WHERE authentication = 'api-key';