SELECT
	employee_id,
	position_id,
	sub,
	COUNT(*) OVER(PARTITION BY position_id, sub) AS `count`
FROM
	%s