SELECT sum(l_extendedprice * l_discount) AS revenue
FROM lineitem
WHERE l_shipdate >= toDate('1994-01-01')
  AND l_shipdate < toDate('1994-01-01') + INTERVAL '1' YEAR
  AND l_discount BETWEEN toFloat64('0.06') - toFloat64('0.01') AND toFloat64('0.06') + toFloat64('0.01')
  AND l_quantity < 24