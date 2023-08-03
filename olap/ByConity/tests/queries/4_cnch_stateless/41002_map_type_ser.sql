CREATE DATABASE IF NOT EXISTS test;

DROP TABLE IF EXISTS test.t;

CREATE TABLE test.t(a Int32, `string_params` Map(String, String))
    ENGINE = CnchMergeTree()
    PARTITION BY `a`
    PRIMARY KEY `a`
    ORDER BY `a`
    SETTINGS index_granularity = 8192;

insert into test.t select a, string_params from test.t;

DROP TABLE IF EXISTS test.t;
