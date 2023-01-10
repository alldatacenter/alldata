SET dialect_type='ANSI';

SELECT a AS b FROM (SELECT 1 AS a) GROUP BY a ORDER BY sum(b); -- { serverError 184 }
SELECT sum(a) AS b FROM (SELECT 1 AS a) ORDER BY sum(b); -- { serverError 184 }
SELECT a AS b FROM (SELECT 1 AS a) ORDER BY rank() OVER (ORDER BY b); -- { serverError 184 }
SELECT sum(a) AS b FROM (SELECT 1 AS a) ORDER BY rank() OVER (ORDER BY b); -- { serverError 184 }
