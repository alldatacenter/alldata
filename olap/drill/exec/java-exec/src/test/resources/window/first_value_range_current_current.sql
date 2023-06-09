SELECT
	employee_id,
	position_id,
	sub,
	FIRST_VALUE(sub) OVER(PARTITION BY position_id ORDER BY sub RANGE BETWEEN CURRENT ROW AND CURRENT ROW) AS `first_value`
FROM
	%s