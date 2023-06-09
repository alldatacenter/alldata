SELECT
	employee_id,
	position_id,
	sub,
	FIRST_VALUE(sub) OVER(PARTITION BY position_id, sub) AS `first_value`
FROM
	%s