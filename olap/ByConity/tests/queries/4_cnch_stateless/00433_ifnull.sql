SELECT ifNull('x', 'y') AS res, toTypeName(ifNull('x', 'y'));
SELECT ifNull(materialize('x'), materialize('y')) AS res, toTypeName(ifNull(materialize('x'), materialize('y')));

SELECT ifNull(toNullable('x'), 'y') AS res, toTypeName(ifNull(toNullable('x'), 'y'));
SELECT ifNull(toNullable('x'), materialize('y')) AS res, toTypeName(ifNull(toNullable('x'), materialize('y')));

SELECT ifNull('x', toNullable('y')) AS res, toTypeName(ifNull('x', toNullable('y')));
SELECT ifNull(materialize('x'), toNullable('y')) AS res, toTypeName(ifNull(materialize('x'), toNullable('y')));

SELECT ifNull(toNullable('x'), toNullable('y')) AS res, toTypeName(ifNull(toNullable('x'), toNullable('y')));

SELECT ifNull(toString(number), toString(-number)) AS res, toTypeName(ifNull(toString(number), toString(-number))) FROM system.numbers LIMIT 5;
SELECT ifNull(nullIf(toString(number), '1'), toString(-number)) AS res, toTypeName(ifNull(nullIf(toString(number), '1'), toString(-number))) FROM system.numbers LIMIT 5;
SELECT ifNull(toString(number), nullIf(toString(-number), '-3')) AS res, toTypeName(ifNull(toString(number), nullIf(toString(-number), '-3'))) FROM system.numbers LIMIT 5;
SELECT ifNull(nullIf(toString(number), '1'), nullIf(toString(-number), '-3')) AS res, toTypeName(ifNull(nullIf(toString(number), '1'), nullIf(toString(-number), '-3'))) FROM system.numbers LIMIT 5;

SELECT ifNull(NULL, 1) AS res, toTypeName(ifNull(NULL, 1));
SELECT ifNull(1, NULL) AS res, toTypeName(ifNull(1, NULL));
SELECT ifNull(NULL, NULL) AS res, toTypeName(ifNull(NULL, NULL));

SELECT IFNULL(NULLIF(toString(number), '1'), NULLIF(toString(-number), '-3')) AS res, toTypeName(IFNULL(NULLIF(toString(number), '1'), NULLIF(toString(-number), '-3'))) FROM system.numbers LIMIT 5;
