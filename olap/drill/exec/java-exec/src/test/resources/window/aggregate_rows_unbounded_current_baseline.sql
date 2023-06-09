SELECT
	COUNT(*) OVER(PARTITION BY position_id ORDER BY sub, employee_id) AS `count`
FROM %s