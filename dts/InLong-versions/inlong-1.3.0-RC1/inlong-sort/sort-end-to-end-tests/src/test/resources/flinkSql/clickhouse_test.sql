CREATE TABLE test_input1 (
    `id` INT primary key,
    name STRING,
    description STRING
) WITH (
    'connector' = 'mysql-cdc-inlong',
    'hostname' = 'mysql',
    'port' = '3306',
    'username' = 'inlong',
    'password' = 'inlong',
    'database-name' = 'test',
    'table-name' = 'test_input1',
    'scan.incremental.snapshot.chunk.size' = '4',
    'scan.incremental.snapshot.enabled' = 'false'
);

CREATE TABLE test_output1 (
    `id` INT primary key,
    name STRING,
    description STRING
) WITH (
    'connector' = 'jdbc-inlong',
    'url' = 'jdbc:clickhouse://clickhouse:8123/default',
    'table-name' = 'test_output1',
    'username' = 'default',
    'password' = '',
    'dialect-impl' = 'org.apache.inlong.sort.jdbc.dialect.ClickHouseDialect'
);

INSERT INTO test_output1 select * from test_input1;



