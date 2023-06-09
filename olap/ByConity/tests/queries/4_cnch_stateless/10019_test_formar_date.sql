SELECT date_format('2019-09-12', 'yyyy');
SELECT date_format('2019-09-12', 'yy');
SELECT date_format('2019-09-12', 'MM');
SELECT date_format('2019-09-12', 'dd');
SELECT date_format('2019-09-12 01:02:03', 'HH');
SELECT date_format('2019-09-12 01:02:03', 'yyyy-MM-dd HH:mm:ss');
SELECT date_format(materialize('2019-09-12 01:02:03'), 'ss');
-- SELECT date_format('2019-09-12 01:02:03', materialize('ss')); -- error 44
