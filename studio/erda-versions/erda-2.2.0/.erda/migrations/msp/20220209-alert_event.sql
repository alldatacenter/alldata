CREATE TABLE sp_alert_event
(
    id                 varchar(32)                        NOT NULL COMMENT '主键' PRIMARY KEY,
    name               varchar(255)                       NOT NULL COMMENT '事件名称（通过ticket模板计算出来）',
    org_id             bigint                             NOT NULL COMMENT '机构ID',
    alert_group_id     varchar(500)                       NOT NULL COMMENT '告警分组id',
    alert_id           bigint                             NOT NULL COMMENT '关联的告警策略ID',
    alert_name         varchar(255)                       NOT NULL COMMENT '关联告警策略名称',
    alert_type         varchar(128)                       NOT NULL COMMENT '告警类型',
    alert_index        varchar(128)                       NOT NULL COMMENT '告警规则index',
    alert_level        varchar(20)                        NOT NULL COMMENT '告警级别',
    alert_source       varchar(20)                        NOT NULL COMMENT '告警来源',
    alert_subject      varchar(500)                       NOT NULL COMMENT '告警对象',
    alert_state        varchar(20)                        NOT NULL COMMENT '告警状态',
    rule_id            bigint                             NOT NULL COMMENT '告警规则id',
    rule_name          varchar(255)                       NOT NULL COMMENT '告警规则名称',
    expression_id      bigint                             NOT NULL COMMENT '关联的告警表达式ID',
    last_trigger_time  datetime                           NOT NULL DEFAULT '0001-01-01' COMMENT '最后触发时间',
    first_trigger_time datetime                           NOT NULL COMMENT '首次触发时间',
    alert_group        varchar(500)                       NOT NULL COMMENT '告警分组',
    scope              varchar(24)                        NOT NULL COMMENT '域',
    scope_id           varchar(64)                        NOT NULL COMMENT '域id',
    created_at         datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at         datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) CHARSET = utf8mb4  COMMENT '告警事件';

CREATE TABLE sp_alert_event_suppress
(
    `id`             varchar(64) NOT NULL COMMENT '主键' PRIMARY KEY,
    `alert_event_id` varchar(64) NOT NULL COMMENT '关联的告警事件id',
    `suppress_type`  varchar(20) NOT NULL COMMENT '抑制类型',
    `expire_time`    datetime    NOT NULL DEFAULT '0001-01-01' COMMENT '失效时间',
    `enabled`        bit         NOT NULL COMMENT '是否启用',
    `created_at`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    `updated_at`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '更新时间',
    CONSTRAINT uk_alert_event_id UNIQUE (alert_event_id)
) CHARSET = utf8mb4  COMMENT '告警事件抑制设置';