create table entities
(
    entity_id varchar(50) not null primary key,
    qualified_name varchar(200) not null,
    entity_type varchar(100) not null,
    attributes NVARCHAR(MAX) not null,
)

create table edges
(
    edge_id   varchar(50) not null primary key,
    from_id   varchar(50) not null,
    to_id     varchar(50) not null,
    conn_type varchar(20) not null,
)