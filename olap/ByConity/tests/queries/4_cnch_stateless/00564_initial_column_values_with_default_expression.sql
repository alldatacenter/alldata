DROP TABLE IF EXISTS test;

CREATE TABLE IF NOT EXISTS test( id UInt32, track UInt8, codec String, content String, rdate Date DEFAULT '2018-02-03', track_id String DEFAULT concat(concat(concat(toString(track), '-'), codec), content) ) ENGINE=CnchMergeTree() PARTITION BY toYYYYMM(rdate) ORDER BY (id, track_id) SETTINGS index_granularity=8192;

INSERT INTO test(id, track, codec) VALUES(1, 0, 'h264');

SELECT * FROM test ORDER BY id;

INSERT INTO test(id, track, codec, content) VALUES(2, 0, 'h264', 'CONTENT');

SELECT * FROM test ORDER BY id;

DROP TABLE IF EXISTS test;
