ALTER TABLE sp_alert_expression
    MODIFY attributes VARCHAR(4096) NOT NULL DEFAULT '' COMMENT '告警规则扩展信息';