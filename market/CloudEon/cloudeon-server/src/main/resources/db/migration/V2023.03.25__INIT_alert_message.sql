create table ce_alert_message
(
    id                       int auto_increment comment '主键'
        primary key,
    alert_name        varchar(32)   null comment '告警指标名',
    alert_group_name         varchar(32)   null comment '告警组',
    alert_info               varchar(1024) null comment '告警详情',
    alert_advice             varchar(1024) null comment '告警建议',
    alert_labels             text null comment '告警消息带的标签json',
    hostname                 varchar(32)   null comment '主机',
    node_id                int   null comment '主机',
    alert_level              int           null comment '告警级别 1：警告2：异常',
    is_resolved               bit(1)            null comment '是否已处理',
    service_role_instance_id int           null comment '集群服务角色实例id',
    service_instance_id      int           null comment '集群服务实例id',
    fire_time              varchar (255)     null comment '开始发出警告的时间',
    solve_time            varchar (255)     null comment '处理时间',
    create_time                    datetime(6)  null,
    update_time                    datetime(6)  null,
    cluster_id               int(10)       null comment '集群id'
)
    comment '集群告警表 ' charset = utf8;

