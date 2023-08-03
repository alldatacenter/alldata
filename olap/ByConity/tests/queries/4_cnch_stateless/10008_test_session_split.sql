DROP TABLE IF EXISTS test_session;
CREATE TABLE test_session (`server_time` UInt64, `time` UInt64, `event` String, `start_time` Nullable(UInt64), `end_time` Nullable(UInt64), `url` Nullable(String), `referer_type` Nullable(String), `referer_site_name` Nullable(String), `hash_uid` UInt64) ENGINE = CnchMergeTree PARTITION BY toYYYYMM(toDateTime(server_time)) ORDER BY time SETTINGS index_granularity = 8192;
-- test split by mSplitTime
insert into test_session values (1567958400, 1567958400, 'predefine_pageview', 0, 0, 'total.colgate.net?test=1_1', 'dir', 'test1_1', 1);
insert into test_session values (1567960200, 1567960200, 'predefine_pageview', 0, 0, 'total.colgate.net?test=1_2', 'inner', 'test1_2', 1);
insert into test_session values (1567962001, 1567962001, 'predefine_pageview', 0, 0, 'total.colgate.net?test=1_3', 'inner', 'test1_3', 1);
-- test split by refer_type
insert into test_session values (1568046600, 1568046600, 'predefine_pageview', 0, 0, 'total.colgate.net?test=2_1', 'inner', 'test2_1', 2);
insert into test_session values (1568047201, 1568047201, 'predefine_pageview', 0, 0, 'total.colgate.net?test=2_2', 'search_engine', 'test2_2', 2);
-- test split by mWindowSize
insert into test_session values (1568159940, 1568159940, 'predefine_pageview', 0, 0, 'total.colgate.net?test=3_1', 'inner', 'test3_1', 3);
insert into test_session values (1568160061, 1568160061, 'predefine_pageview', 0, 0, 'total.colgate.net?test=3_2', 'inner', 'test3_2', 3);
-- test only _be_active
insert into test_session values (1568249940, 1568249940, '_be_active', 1568249940*1000, 1568249940*1000, 'total.colgate.net?test=4_1', 'inner', 'test4_1', 4);
insert into test_session values (1568250061, 1568250061, '_be_active', 1568250061*1000, 1568250061*1000, 'total.colgate.net?test=4_2', 'inner', 'test4_2', 4);
-- test start by _be_active
insert into test_session values (1568422861, 1568422861, '_be_active', 1568422861*1000, 1568424541*1000, 'total.colgate.net?test=6_1', 'inner', 'test6_1', 6);
insert into test_session values (1568426340, 1568426340, 'predefine_pageview', 0, 0, 'total.colgate.net?test=6_2', 'inner', 'test6_2', 6);
insert into test_session values (1568426461, 1568426461, '_be_active', 1568426461*1000, 1568426521*1000, 'total.colgate.net?test=6_3', 'inner', 'test6_3', 6);

SELECT '--------first sessionSplit(1)--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 0)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 0)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name, start_time), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3; -- { serverError 43 }
SELECT '--------first sessionSplit(2)--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 0)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name, url, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 0)(server_time, event, time, start_time, end_time, time, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3; -- { serverError 43 }
SELECT '--------first sessionSplitR2--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplitR2(1800, 86400, 0, 0)(server_time, event, time, start_time, end_time, url, referer_type), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;

SELECT '--------last sessionSplit(1)--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 1)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;
SELECT '--------last sessionSplit(2)--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplit(1800, 86400, 0, 1)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name, url, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;
SELECT '--------last sessionSplitR2--------';
SELECT * FROM (SELECT arrayJoin(ifnull(sessionSplitR2(1800, 86400, 0, 1)(server_time, event, time, start_time, end_time, url, referer_type), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.3;

SELECT '--------pageTime--------';
SELECT * FROM (SELECT arrayJoin(ifnull(pageTime(1800, 86400, 0)(server_time, event, time, start_time, end_time, url, referer_type), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.1;
SELECT '--------pageTime2--------';
SELECT * FROM (SELECT arrayJoin(ifnull(pageTime2(1800, 86400, 0)(server_time, event, time, start_time, end_time, url, referer_type, referer_site_name), [])) AS pt FROM test_session GROUP BY hash_uid ORDER BY hash_uid) ORDER BY pt.1;

DROP TABLE test_session;