alter table sp_alert_event_suppress
    add column `org_id`   bigint      not null comment '组织ID',
    add column `scope`    varchar(20) not null comment '域',
    add column `scope_id` varchar(64) not null comment '域ID';