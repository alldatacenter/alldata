-- require enable_replace_group_by_literal_to_symbol enabled
SET dialect_type='ANSI';

SELECT a FROM (SELECT 1 AS a) GROUP BY 1;
SELECT sum(a) FROM (SELECT 1 AS a) GROUP BY 1; -- { serverError 184 }
SELECT 1 + sum(a) FROM (SELECT 1 AS a) GROUP BY 1; -- { serverError 184 }
SELECT rank() OVER (ORDER BY a) FROM (SELECT 1 AS a) GROUP BY 1; -- { serverError 184 }
SELECT grouping(a) FROM (SELECT 1 AS a) GROUP BY 1; -- { serverError 184 }
SELECT (SELECT sum(b) FROM (SELECT 2 AS b)) FROM (SELECT 1 AS a) GROUP BY 1;
