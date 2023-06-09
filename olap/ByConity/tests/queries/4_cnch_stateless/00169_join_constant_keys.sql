SET enable_shuffle_with_order = 1;
SELECT
    key1,
    key2,
    x.table_1
FROM
(
    SELECT
        arrayJoin([1, 2, 3]) AS key1,
        0 AS key2,
        999 AS table_1
) x ALL INNER JOIN
(
    SELECT
        arrayJoin([1, 3, 2]) AS key1,
        0 AS key2,
        999 AS table_1
) y USING key2, key1;
