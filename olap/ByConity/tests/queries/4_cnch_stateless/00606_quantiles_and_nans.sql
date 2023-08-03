SELECT DISTINCT
    eq
FROM
    (
        SELECT
            number,
            arrayReduce('quantileExact', arrayMap(x -> x = intDiv(number, 10) ? nan : x, range(2 + number % 10))) AS q1,
            arrayReduce('quantileExact', arrayFilter(x -> x != intDiv(number, 10), range(2 + number % 10))) AS q2,
            q1 = q2 AS eq
        FROM
            numbers(100)
    );
