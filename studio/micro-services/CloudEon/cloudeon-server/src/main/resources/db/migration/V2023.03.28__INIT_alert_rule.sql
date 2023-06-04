create table ce_stack_alert_rule
(
    id                int        auto_increment  primary key,
    rule_name         varchar(255) null,
    alert_level       int          null,
    promql            text         null,
    stack_service_name       varchar(255)          null,
    stack_role_name varchar(255)           null,
    wait_for_fire_duration          int          null,
    alert_info text null,
    alert_advice text null
);

create table ce_alert_rule_define
(
    id                int           auto_increment  primary key,
    cluster_id                int          null,
    rule_name         varchar(255) null,
    alert_level       int          null,
    promql            text         null,
    stack_service_name       varchar(255)          null,
    stack_role_name varchar(255)           null,
    wait_for_fire_duration          int          null,
    create_time       datetime      null comment '创建时间',
    update_time       datetime      null comment '更新时间',
    alert_info text null,
    alert_advice text null
);