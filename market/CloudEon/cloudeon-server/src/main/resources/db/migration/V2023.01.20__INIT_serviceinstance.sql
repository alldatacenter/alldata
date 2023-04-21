



create table ce_service_instance_config
(
    id                int auto_increment
        primary key,
    create_time       datetime(6)  null,
    name              varchar(255) null,
    node_id           int          null,
    recommended_value text null,
    conf_file varchar(255) null,
    service_instance_id        int          null,
    update_time       datetime(6)  null,
    is_custom_conf   bit          not null,
    user_id           int          null,
    value             text null

);

create table ce_service_instance
(
    id                              int auto_increment
        primary key,
    cluster_id                      int          null,
    create_time                     datetime(6)  null,
    dependence_service_instance_ids varchar(255) null,
    enable_kerberos                 bit           null,
    label                           varchar(255) null,
    need_restart                    bit           null,
    service_name                    varchar(255) null,
    service_state                   int          null,
    instance_sequence                  int          null,
    persistence_paths varchar(1024) null,
    stack_service_id                int          null,
    update_time                     datetime(6)  null
);

create table ce_service_role_instance
(
    id                  int auto_increment
        primary key,
    cluster_id          int          null,
    create_time         datetime(6)  null,
    is_decommission     bit          not null,
    need_restart        bit          not null,
    node_id             int          null,
    role_type           varchar(255)          null,
    stack_service_role_id          int          null,
    service_instance_id int          null,
    service_role_name   varchar(255) null,
    service_role_state  int          null,
    update_time         datetime(6)  null
);

create table ce_service_role_instance_webuis
(
    id                       int auto_increment
        primary key,
    name                     varchar(255) null,
    service_instance_id      int          null,
    service_role_instance_id int          null,
    web_host_url             varchar(255) null,
    web_ip_url               varchar(255) null
);

create table ce_user
(
    id          int auto_increment
        primary key,
    create_time datetime(6)  null,
    email       varchar(255) null,
    password    varchar(255) null,
    phone       varchar(255) null,
    user_type   int          null,
    username    varchar(255) null
);

create table ce_session
(
    id              varchar(255) not null
        primary key,
    ip              varchar(255) null,
    last_login_time datetime(6)  null,
    user_id         int          null
);

