create table ce_stack_service_conf
(
    id                    int auto_increment
        primary key,
    name             varchar(255) null,
    label    varchar(255) null,
    description    varchar(255) null,
    value_type    varchar(255) null,
    recommend_expression     text null,
    service_id            int          null,
    conf_file              varchar(255) null,
    configurable_in_wizard   bit null,
    stack_id              int          null,
    min     int          null,
    max int null ,
    unit varchar(255) null,
    options varchar(255) null,
    is_password  bit          not null,
    is_multi_value  bit          not null
);

