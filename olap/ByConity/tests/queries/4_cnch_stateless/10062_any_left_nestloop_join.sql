SET enable_nested_loop_join = 1;
SET enable_shuffle_with_order = 1;

SELECT * FROM
(
    SELECT number AS k FROM system.numbers LIMIT 10
) js1
ANY LEFT JOIN
(
    SELECT intDiv(number, 2) AS k, number AS joined FROM system.numbers LIMIT 10
) js2
on js1.k > js2.k;