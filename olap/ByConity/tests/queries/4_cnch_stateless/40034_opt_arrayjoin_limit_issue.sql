SET enable_optimizer = 1;

SELECT x, arrayJoin(['a', 'b', 'c']) FROM (SELECT 1 as x) LIMIT 1;
EXPLAIN SELECT x, arrayJoin(['a', 'b', 'c']) FROM (SELECT 1 as x) LIMIT 1;
