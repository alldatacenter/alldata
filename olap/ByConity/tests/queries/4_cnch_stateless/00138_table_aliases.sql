SELECT * FROM `system`.`one` AS `xxx`;
SELECT 1 AS k, s FROM (SELECT 1 AS k FROM `system`.`one` AS `xxx`) AS `zzz` ANY INNER JOIN (SELECT 1 AS k, 'Hello' AS s) AS `yyy` USING k;
