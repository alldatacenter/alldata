SET enable_shuffle_with_order = 1;
SELECT x, y FROM (SELECT number AS x FROM system.numbers LIMIT 3) CROSS JOIN (SELECT number AS y FROM system.numbers LIMIT 5);
