CREATE TABLE data_gen (
    amount BIGINT
) WITH (
    'connector' = 'datagen',
    'rows-per-second' = '1',
    'number-of-rows' = '3',
    'fields.amount.kind' = 'random',
    'fields.amount.min' = '10',
    'fields.amount.max' = '11');
CREATE TABLE mysql_sink (
  amount BIGINT,
  PRIMARY KEY (amount) NOT ENFORCED
) WITH (
    'connector' = 'jdbc',
    'url' = 'jdbc:mysql://localhost:3306/test_db',
    'table-name' = 'test_table',
    'username' = 'root',
    'password' = '123456',
    'lookup.cache.max-rows' = '5000',
    'lookup.cache.ttl' = '10min'
);
INSERT INTO mysql_sink SELECT amount as amount FROM data_gen;