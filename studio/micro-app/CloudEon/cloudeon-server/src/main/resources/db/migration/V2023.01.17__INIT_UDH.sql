create table ce_cluster_info
(
    id           int auto_increment
        primary key,
    cluster_code varchar(255) null,
    cluster_name varchar(255) null,
    create_by    varchar(255) null,
    create_time  datetime(6)  null,
    stack_id     int          null
);


create table ce_cluster_node
(
    id               int auto_increment
        primary key,
    cluster_id       int          null,
    core_num         int          null,
    cpu_architecture varchar(255) null,
    create_time      datetime(6)  null,
    hostname         varchar(255) null,
    ip               varchar(255) null,
    node_label       varchar(255) null,
    rack             varchar(255) null,
    service_role_num int          null,
    ssh_password     varchar(255) null,
    ssh_port         int          null,
    ssh_user         varchar(255) null,
    total_disk       int          null,
    total_mem        int          null
);


create table ce_stack_info
(
    id         int auto_increment
        primary key,
    stack_code varchar(255) null
);

create table ce_stack_service
(
    id                             int auto_increment
        primary key,
    name                   varchar(255) null,
    version                varchar(255) null,
    run_as                varchar(255) null,
    support_kerberos bit null,
    label                varchar(255) null,
    description                   varchar(1024) null,
    custom_config_files            varchar(255) null,
    dependencies                   varchar(255) null,
    docker_image                   varchar(255) null,
    pages                          varchar(255) null,
    service_configuration_yaml     text null,
    service_configuration_yaml_md5 varchar(255) null,
    service_role_yaml     text null,
    service_role_yaml_md5 varchar(255) null,
    persistence_paths varchar(1024) null,
    sort_num                       int          null,
    stack_code                     varchar(255) null,
    stack_id                       int          null
);

create table ce_stack_service_role
(
    id                    int auto_increment
        primary key,
    label    varchar(255) null,
    role_full_name     varchar(255) null,
    name             varchar(255) null,
    service_id            int          null,
    assign                varchar(255) null,
    frontend_operations   varchar(255) null,
    jmx_port              varchar(255) null,
    link_expression       varchar(255) null,
    type     varchar(255) null,
    sort_num              int          null,
    min_num              int          null,
    fixed_num              int          null,
    need_odd              bit          not null,
    stack_id              int          null
);


