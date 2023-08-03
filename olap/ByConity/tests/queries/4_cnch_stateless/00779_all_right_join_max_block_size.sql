SET max_block_size = 6;
SELECT blockSize() bs FROM (SELECT 1 s) x ALL RIGHT JOIN (SELECT arrayJoin([2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3]) s) y ON x.s = y.s GROUP BY blockSize() ORDER BY bs;
