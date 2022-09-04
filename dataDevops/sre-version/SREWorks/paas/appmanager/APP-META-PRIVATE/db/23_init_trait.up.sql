create table am_trait
(
    id                   bigint auto_increment
        primary key,
    gmt_create           datetime     null,
    gmt_modified         datetime     null,
    name                 varchar(128) not null,
    class_name           varchar(255) not null,
    definition_ref       varchar(128) null,
    trait_definition     longtext     null,
    constraint am_trait_name_uindex unique (name)
);

create index idx_class_name
    on am_trait (class_name);

create index idx_definition_ref
    on am_trait (definition_ref);

create index idx_gmt_create
    on am_trait (gmt_create);

create index idx_gmt_modified
    on am_trait (gmt_modified);

