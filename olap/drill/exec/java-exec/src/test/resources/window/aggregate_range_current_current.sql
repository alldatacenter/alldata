SELECT
	employee_id,
	position_id,
	sub,
	COUNT(*) OVER(PARTITION BY position_id ORDER BY sub RANGE BETWEEN CURRENT ROW AND CURRENT ROW) AS `count`
FROM
	%s