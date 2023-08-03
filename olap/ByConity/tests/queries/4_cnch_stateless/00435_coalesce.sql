SELECT coalesce(), coalesce(NULL), coalesce(NULL, NULL),
    coalesce(1), coalesce(1, NULL), coalesce(NULL, 1), coalesce(NULL, 1, NULL);

SELECT COALESCE(), COALESCE(NULL), COALESCE(1, NULL);

SELECT res, toTypeName(res) FROM (SELECT coalesce(number % 2 = 0 ? number : NULL, number % 3 = 0 ? number : NULL, number % 5 = 0 ? number : NULL) AS res FROM system.numbers LIMIT 15);
SELECT res, toTypeName(res) FROM (SELECT coalesce(number % 2 = 0 ? number : NULL, number % 3 = 0 ? number : NULL, number) AS res FROM system.numbers LIMIT 15);
SELECT res, toTypeName(res) FROM (SELECT coalesce(number % 2 = 0 ? number : NULL, number % 3 = 0 ? number : NULL, 100) AS res FROM system.numbers LIMIT 15);
