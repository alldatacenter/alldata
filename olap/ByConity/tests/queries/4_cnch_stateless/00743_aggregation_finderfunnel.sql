USE test;

DROP TABLE IF EXISTS funnel_test;
CREATE TABLE funnel_test (uid UInt32 default 1, timestamp UInt64, event UInt32, prop String) engine=CnchMergeTree order by uid;
INSERT INTO funnel_test (timestamp, event, prop) values (86400,1000,'a'),(86401,1001,'b'),(86402,1002,'c'),(86403,1003,'d'),(86404,1004,'e'),(86405,1005,'f'),(86406,1006,'g');

SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(1, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(2, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(3, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1010) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 86400, 86400, 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1006) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 0, 86400, 2)(timestamp, timestamp, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;

DROP TABLE funnel_test;

CREATE TABLE funnel_test (uid UInt32 default 1, timestamp UInt64, event UInt32, prop String) engine=CnchMergeTree order by uid;
INSERT INTO funnel_test (timestamp, event, prop) values (129600,1000,'a'),(143997,1000, 'a'),(143998,1001,'b'),(143999,1003,'d'),(144000,1002,'c'),(172799,1000,'a'),(172800,1003,'d'),(172801,1000,'a'),(172802,1001,'b'),(172803,1002,'c'),(172804,1003,'d'),(172805,1004,'f'),(216000,1000,'a'),(216001,1001,'b'),(216002,1001,'b'),(216003,1002,'c');

-- tips
-- timestamp            utc-0                   utc-8
-- 129600              1970-01-02 12:00:00     1970-01-02 20:00:00
-- 144000              1970-01-02 04:00:00     1970-01-03 00:00:00
-- 172800              1970-01-03 00:00:00     1970-01-03 08:00:00
-- 216000              1970-01-03 12:00:00     1970-01-03 20:00:00
SELECT uid, finderFunnel(86400, 129600, 86400, 1)(timestamp, timestamp, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 1, 1, 'Asia/Shanghai')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 1, 1, 'Etc/GMT')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 129600, 86400, 1)(timestamp, timestamp, event = 1000, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 1, 1, 'Asia/Shanghai')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 1, 1, 'Etc/GMT')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 129600, 86400, 2)(timestamp, timestamp, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 2, 1, 'Asia/Shanghai')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 129600, 86400, 2, 1, 'Etc/GMT')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;

SELECT uid, finderFunnel(86400, 129600, 86400, 2)(timestamp, timestamp, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 57600, 86400, 2, 1, 'Asia/Shanghai')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(86400000, 86400, 86400, 2, 1, 'Etc/GMT')(timestamp, timestamp*1000, event = 1000, event = 1001, event = 1002) FROM funnel_test GROUP BY uid;

DROP TABLE IF EXISTS funnel_test;
CREATE TABLE funnel_test (uid UInt32 default 1, timestamp UInt64, event UInt32, prop String) engine=CnchMergeTree order by uid;
INSERT INTO funnel_test (timestamp, event, prop) values
(86400, 1001, 'a'),(86401, 1002, 'b'), (86402, 1001, 'b'), (86403, 1003, 'b'), (86404, 1001, 'a'), (86405, 1001, 'd'), (86406, 1002, 'd'), (86407, 1003, 'b'), (86408, 1004, 'd'), (86410, 1003, 'f'), (86413, 1001, 'w');

SELECT uid, finderFunnel(5, 86400, 1, 14, 7)(timestamp, timestamp, prop, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 5)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 6)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 10)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 14)(timestamp, timestamp, prop, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;


SELECT uid, finderFunnel(5, 86400, 1, 14, 7, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, prop, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 5, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 6, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 10, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 14, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, prop, prop, prop, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;
SELECT uid, finderFunnel(5, 86400, 1, 14, 0, 0, 'Asia/Shanghai', 1)(timestamp, timestamp, event = 1001, event = 1002, event = 1003, event = 1004) FROM funnel_test GROUP BY uid;

DROP TABLE funnel_test;

-- for step execute
