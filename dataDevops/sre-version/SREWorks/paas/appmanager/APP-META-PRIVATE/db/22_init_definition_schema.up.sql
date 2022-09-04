create table am_definition_schema
(
    id           bigint auto_increment
        primary key,
    gmt_create   datetime null,
    gmt_modified datetime null,
    name         varchar(128) not null,
    json_schema  longtext     not null,
    constraint am_definition_schema_name_uindex
        unique (name)
);

