SET enable_nested_loop_join = 1;
SET enable_shuffle_with_order = 1;

SELECT a.*, b.* FROM
(
    SELECT number AS k FROM system.numbers LIMIT 10
) AS a
ALL INNER JOIN
(
    SELECT intDiv(number, 2) AS k, number AS joined FROM system.numbers LIMIT 10
) AS b
on a.k > b.k;