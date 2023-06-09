
DROP TABLE IF EXISTS test_date;
CREATE TABLE test_date (`a` DateTime, `b` DateTime, `c` Date, `d` Date) ENGINE = CnchMergeTree PARTITION BY c ORDER BY d;
insert into test_date values (now(), now() - interval 1 day, today(), yesterday());
insert into test_date values (toDateTime('2019-01-01 00:00:00'), toDateTime('2019-08-25 14:00:00'), toDate('2019-01-01 00:00:00'), toDate('2019-08-25 14:00:00'));

SELECT dateDiff(a, b) AS diff FROM test_date ORDER BY diff;
SELECT dateDiff('day', a, b) AS diff FROM test_date ORDER BY diff;
SELECT dateDiff(c, d) AS diff FROM test_date ORDER BY diff;
SELECT dateDiff(b, d) AS diff FROM test_date ORDER BY diff;
DROP TABLE test_date;

SELECT dateDiff(toDate('2019-12-31'), toDate('2019-01-01'));
SELECT dateDiff(toDate('2019-12-31'), toDate('2019-01-01'));
SELECT dateDiff(toDate('2019-12-31'), toDate('2020-01-01'));
SELECT dateDiff(materialize(toDate('2019-12-31')), toDate('2018-01-01'));
SELECT dateDiff(toDate('2019-12-31'), materialize(toDate('2019-01-01')));
SELECT dateDiff(materialize(toDate('2019-12-31')), materialize(toDate('2020-01-01')));
SELECT DATEDIFF(today(), today() - INTERVAL 10 DAY);
SELECT dateDiff(toDate('2019-10-26'), toDate('2019-10-27'), 'Asia/Shanghai');
SELECT dateDiff(toDate('2014-10-26'), toDate('2014-10-27'), 'UTC');
SELECT dateDiff(toDateTime('2019-10-26 00:00:00', 'Asia/Shanghai'), toDateTime('2019-10-27 00:00:00', 'Asia/Shanghai'));
SELECT dateDiff(toDateTime('2019-10-26 00:00:00', 'UTC'), toDateTime('2019-10-27 00:00:00', 'UTC'));

select dateDiff('day', today()); -- { serverError 42 }
select dateDiff(today()); -- { serverError 42 }

