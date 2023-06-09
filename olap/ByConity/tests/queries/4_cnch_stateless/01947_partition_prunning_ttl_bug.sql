-- Can use partition prunning with TTL before fetching
CREATE TABLE t (c_date Date,c_datetime DateTime64(3) ) ENGINE = CnchMergeTree PARTITION BY c_date ORDER BY c_date TTL toDate(c_date) + toIntervalDay(30);
INSERT INTO t VALUES ('2022-04-14', '2023-02-14 13:16:17');
SELECT * FROM t SETTINGS max_bytes_to_read=1; -- Read nothing

-- Cannot use partition prunning with TTL before fetching
CREATE TABLE t_bug (c_date Date,c_datetime DateTime64(3) ) ENGINE = CnchMergeTree PARTITION BY toYYYYMMDD(c_date) ORDER BY c_date TTL toDate(c_date) + toIntervalDay(30);
INSERT INTO t_bug VALUES ('2023-04-14', '2023-04-14 13:16:17');
SELECT * FROM t_bug;