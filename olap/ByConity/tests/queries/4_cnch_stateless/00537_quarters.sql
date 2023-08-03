SELECT d, t, toQuarter(d) AS qd, toQuarter(t) AS qt, toStartOfQuarter(d) AS sqd, toStartOfQuarter(t) AS sqt, toRelativeQuarterNum(d) - toRelativeQuarterNum(base) AS qdiff_d, toRelativeQuarterNum(t) - toRelativeQuarterNum(base) as qdiff_t
FROM
    (
        SELECT
            toDate('2017-01-01') AS base,
            toDate('2017-01-01') + INTERVAL number MONTH AS d,
            toDateTime(toDate('2017-01-01') + INTERVAL number MONTH) AS t
        FROM system.numbers LIMIT 24
    );
