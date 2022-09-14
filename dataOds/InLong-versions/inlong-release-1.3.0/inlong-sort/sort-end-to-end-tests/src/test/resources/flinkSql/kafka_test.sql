CREATE TABLE test_input (
    `id` INT NOT NULL,
    name STRING,
    description STRING,
    weight DECIMAL(10,3),
    enum_c STRING,
    json_c STRING,
    point_c STRING
) WITH (
    'connector' = 'mysql-cdc-inlong',
    'hostname' = 'mysql',
    'port' = '3306',
    'username' = 'inlong',
    'password' = 'inlong',
    'database-name' = 'test',
    'table-name' = 'test_input',
    'append-mode' = 'true',
    'scan.incremental.snapshot.chunk.size' = '4',
    'scan.incremental.snapshot.enabled' = 'false'
);

CREATE TABLE kafka_load (
    `id` INT NOT NULL,
    name STRING,
    description STRING,
    weight DECIMAL(10,3),
    enum_c STRING,
    json_c STRING,
    point_c STRING
) WITH (
    'connector' = 'kafka-inlong',
    'topic' = 'test-topic',
    'properties.bootstrap.servers' = 'kafka:9092',
    'format' = 'csv'
);

CREATE TABLE kafka_extract (
    `id` INT NOT NULL,
    name STRING,
    description STRING,
    weight DECIMAL(10,3),
    enum_c STRING,
    json_c STRING,
    point_c STRING
) WITH (
    'connector' = 'kafka-inlong',
    'topic' = 'test-topic',
    'properties.bootstrap.servers' = 'kafka:9092',
    'properties.group.id' = 'testGroup',
    'scan.startup.mode' = 'earliest-offset',
    'format' = 'csv'
);

CREATE TABLE test_output (
    `id` INT NOT NULL,
    name STRING,
    description STRING,
    weight DECIMAL(10,3),
    enum_c STRING,
    json_c STRING,
    point_c STRING
) WITH (
    'connector' = 'jdbc-inlong',
    'url' = 'jdbc:mysql://mysql:3306/test',
    'table-name' = 'test_output',
    'username' = 'inlong',
    'password' = 'inlong'
);

INSERT INTO kafka_load select * from test_input;
INSERT INTO test_output select * from kafka_extract;




